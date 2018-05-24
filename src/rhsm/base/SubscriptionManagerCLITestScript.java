package rhsm.base;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;

import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.Org;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;
import rhsm.data.Translation;
import rhsm.data.YumRepo;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
public class SubscriptionManagerCLITestScript extends SubscriptionManagerBaseTestScript {
	
	protected static SubscriptionManagerTasks clienttasks	= null;
	protected static SubscriptionManagerTasks client1tasks	= null;	// client1 subscription manager tasks
	protected static SubscriptionManagerTasks client2tasks	= null;	// client2 subscription manager tasks
	
	public static Random randomGenerator = new Random(System.currentTimeMillis());
	
	public SubscriptionManagerCLITestScript() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeSuite(groups={"setup"},description="subscription manager set up")
	public void setupBeforeSuite() throws IOException, JSONException {
		if (isSetupBeforeSuiteComplete) return;
		
		// create SSHCommandRunners to connect to the subscription-manager clients
		File sshKeyPrivateKeyFile = new File(System.getProperty("automation.dir", null)+"/"+sm_sshKeyPrivate);
		if (!sshKeyPrivateKeyFile.exists()) Assert.fail("Expected to find the private ssh key for automation testing at '"+sshKeyPrivateKeyFile+"'.  Ask the RHSM Automation Administrator for a copy.");
		client1 = new SSHCommandRunner(sm_client1Hostname, sm_client1SSHUser, new File(sm_sshKeyPrivate), sm_sshkeyPassphrase, null);
		if (sm_sshEmergenecyTimeoutMS!=null) client1.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
		client1tasks = new SubscriptionManagerTasks(client1);
		client = client1;
		clienttasks = client1tasks;
		
		// will we be testing multiple clients?
		if (!(	sm_client2Hostname.equals("") /*|| client2username.equals("") || client2password.equals("")*/ )) {
			client2 = new SSHCommandRunner(sm_client2Hostname, sm_client2SSHUser, new File(sm_sshKeyPrivate), sm_sshkeyPassphrase, null);
			if (sm_sshEmergenecyTimeoutMS!=null) client2.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
			client2tasks = new SubscriptionManagerTasks(client2);
		} else {
			log.info("Multi-client testing will be skipped.");
		}
		
		// unregister clients in case they are still registered from prior run (DO THIS BEFORE SETTING UP A NEW CANDLEPIN)
		unregisterClientsAfterSuite();
		
		// assert that fips is enabled (or not) as expected before running any tests.
		// FIPS INFO:
	    //	https://beaker.engineering.redhat.com/jobs/1259246
		//	Notice this task... <task name="/distribution/fips/setup-fips-enabled" role="STANDALONE"/>
		//	How can I make RHEL 6 or RHEL 7 FIPS 140-2 compliant?  https://access.redhat.com/solutions/137833
		// 
		// Beaker test_log-Setup.log :: [   FAIL   ] :: FIPS on s390 on RHEL <7.1 is not supported (Assert: expected 0, got 1)
		if (client1 != null) {
			Assert.assertEquals(client1.runCommandAndWait("sysctl crypto.fips_enabled").getStdout().trim(), "crypto.fips_enabled = "+(sm_clientFips?"1":"0"), "Asserting the expected enablement of FIPS on client '"+sm_client1Hostname+"' before running any tests.");
		}
		if (client2 != null) {
			Assert.assertEquals(client2.runCommandAndWait("sysctl crypto.fips_enabled").getStdout().trim(), "crypto.fips_enabled = "+(sm_clientFips?"1":"0"), "Asserting the expected enablement of FIPS on client '"+sm_client2Hostname+"' before running any tests.");
		}
		
		
		List<File> generatedProductCertFiles = new ArrayList<File>();
		
		// can we create an SSHCommandRunner to connect to the candlepin server ?
		if (!sm_serverHostname.equals("") && sm_serverType.equals(CandlepinType.standalone)) {
			server = new SSHCommandRunner(sm_serverHostname, sm_serverSSHUser, new File(sm_sshKeyPrivate), sm_sshkeyPassphrase, null);
			if (sm_sshEmergenecyTimeoutMS!=null) server.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
			servertasks = new rhsm.cli.tasks.CandlepinTasks(server,sm_serverInstallDir,sm_serverImportDir,sm_serverType,sm_serverBranch,sm_dbSqlDriver,sm_dbHostname,sm_dbPort,sm_dbName,sm_dbUsername,sm_dbPassword);
		} else {
			log.info("Assuming the server is already setup and running.");
			servertasks = new rhsm.cli.tasks.CandlepinTasks(null,null,null,sm_serverType,sm_serverBranch,sm_dbSqlDriver,sm_dbHostname,sm_dbPort,sm_dbName,sm_dbUsername,sm_dbPassword);
		}
		
		// setup the candlepin server (only when the candlepin server is standalone)
		if (server!=null && sm_serverType.equals(CandlepinType.standalone)) {
			
			// re-configure candlepin.conf to standalone true
			servertasks.uncommentConfFileParameter("candlepin.standalone");
			if (servertasks.getConfFileParameter("candlepin.standalone")==null) servertasks.addConfFileParameter ("\n# standalone true (default) is indicative of a Satellite deployment versus false which is indicative of the Customer Portal (RHSMQE was here)\n"+"candlepin.standalone", "true");	// default - indicative of a Satellite deployment
			servertasks.updateConfFileParameter("candlepin.standalone", "true");
			
			// re-configure candlepin.conf to purge the candlepin CRL list every 2 min
			// TODO: If the tests that depend on this config are isolated to one class, then it would be smarter to dynamically set this and restartTomcat() within a BeforeClass and AfterClass
			String candlepinConfFileParameter = "pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule";	// applicable on candlepin-0.4
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.5")) {
				candlepinConfFileParameter = "pinsetter.org.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule";	// applicable on candlepin-0.5
			}
			servertasks.uncommentConfFileParameter(candlepinConfFileParameter);
			if (servertasks.getConfFileParameter(candlepinConfFileParameter)==null) servertasks.addConfFileParameter("\n# purge the candlepin CRL list every 2 min (defaults to once a day at noon  0 0 12 * * ?) (RHSMQE was here)\n"+candlepinConfFileParameter,"0 0/2 * * * ?");
			servertasks.updateConfFileParameter(candlepinConfFileParameter,"0 0\\/2 * * * ?");  // every 2 minutes
			
			// re-configure candlepin.conf to purge candlepin of expired pools every 2 min (defaults to one hour)
			// TODO: If the tests that depend on this config are isolated to one class, then it would be smarter to dynamically set this and restartTomcat() within a BeforeClass and AfterClass
			candlepinConfFileParameter = "pinsetter.org.candlepin.pinsetter.tasks.ExpiredPoolsJob.schedule";
			servertasks.uncommentConfFileParameter(candlepinConfFileParameter);
			if (servertasks.getConfFileParameter(candlepinConfFileParameter)==null) servertasks.addConfFileParameter("\n# purge candlepin of expired pools every 2 min (defaults to one hour) (RHSMQE was here)\n"+candlepinConfFileParameter,"0 0/2 * * * ?");
			servertasks.updateConfFileParameter(candlepinConfFileParameter,"0 0\\/2 * * * ?");  // every 2 minutes
			
			// re-configure candlepin.conf with a secret consumer name to trigger the canActivate attribute
			// TODO: If the tests that depend on this config are isolated to one class, then it would be smarter to dynamically set this and restartTomcat() within a BeforeClass and AfterClass
			candlepinConfFileParameter = "candlepin.subscription.activation.debug_prefix";
			servertasks.uncommentConfFileParameter(candlepinConfFileParameter);
			if (servertasks.getConfFileParameter(candlepinConfFileParameter)==null) servertasks.addConfFileParameter("\n# secret consumer name to trigger the canActivate attribute (RHSMQE was here)\n"+candlepinConfFileParameter,"redeem");
			servertasks.updateConfFileParameter(candlepinConfFileParameter,"redeem");
			
			// re-configure candlepin.conf with INFO logging to keep down the logs
			/*
			candlepinConfFileParameter = "log4j.logger.org.candlepin.policy.js.compliance";
			servertasks.uncommentConfFileParameter(candlepinConfFileParameter);
			if (servertasks.getConfFileParameter(candlepinConfFileParameter)==null) servertasks.addConfFileParameter("\n# (RHSMQE was here)\n"+candlepinConfFileParameter,"INFO");
			servertasks.updateConfFileParameter(candlepinConfFileParameter,"INFO");
			candlepinConfFileParameter = "log4j.logger.org.candlepin";
			servertasks.uncommentConfFileParameter(candlepinConfFileParameter);
			if (servertasks.getConfFileParameter(candlepinConfFileParameter)==null) servertasks.addConfFileParameter("\n# (RHSMQE was here)\n"+candlepinConfFileParameter,"INFO");
			servertasks.updateConfFileParameter(candlepinConfFileParameter,"INFO");
			*/
			
			// After all of the candlepin.conf updates above, the server needs to be restarted; let's assume deploy() will take care of that.
			
			servertasks.cleanOutCRL();
			servertasks.deploy();
			server.runCommandAndWait("df -h");
			server.runCommandAndWait("sudo "+"ls -Slh "+servertasks.getTomcatLogFile().getParent()+" | head");	// log candlepin's starting disk usage (for debugging information only)
			servertasks.setupTranslateToolkitFromTarUrl(sm_translateToolkitTarUrl);
			servertasks.reportAPI();
			
			// install packages
			SSHCommandResult yumInstallResult = server.runCommandAndWait("sudo "+"yum install -y --quiet hunspell");
			Assert.assertEquals(yumInstallResult.getExitCode(), Integer.valueOf(0),"ExitCode from yum install of packages on server '"+server.getConnection().getRemoteHostname()+"'.");
			
			// fetch the generated Product Certs
			if (Boolean.valueOf(getProperty("sm.debug.fetchProductCerts","true"))) {
			log.info("Fetching the generated product certs...");
			//SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(server, "find "+serverInstallDir+servertasks.generatedProductsDir+" -name '*.pem'", 0);
			//SSHCommandResult result = server.runCommandAndWait("find "+sm_serverInstallDir+servertasks.generatedProductsDir+" -name '*.pem'");	// find /root/candlepin/server/generated_certs -name '*.pem'
			SSHCommandResult result = server.runCommandAndWait("find "+sm_serverInstallDir+" -name '*.pem' | grep generated_certs");	// find /home/candlepin/candlepin -name '*.pem' | grep generated_certs
			String[] remoteFilesAsString = result.getStdout().trim().split("\\n");
			if (remoteFilesAsString.length==1 && remoteFilesAsString[0].equals("")) remoteFilesAsString = new String[]{};
			if (remoteFilesAsString.length==0) log.warning("No generated product certs were found on the candlpin server for use in testing.");
			for (String remoteFileAsString : remoteFilesAsString) {
				File remoteFile = new File(remoteFileAsString);
				File localFile = new File((getProperty("automation.dir", "/tmp")+"/tmp/"+remoteFile.getName()).replace("tmp/tmp", "tmp"));
				File localFileRenamed = new File(localFile.getPath().replace(".pem", "_.pem")); // rename the generated productCertFile to help distinguish it from a true RHEL productCertFiles
				RemoteFileTasks.getFile(server, localFile.getParent(),remoteFile.getPath());
				localFile.renameTo(localFileRenamed);
				generatedProductCertFiles.add(localFileRenamed);
			}
			}
		}
		
		// fetch the candlepin CA Cert
		File serverCaCertFile = serverCaCertFile = fetchServerCaCertFile();
		
		// setup the client(s) (with the fetched candlepin CA Cert and the generated product certs)
		for (SubscriptionManagerTasks smt : new SubscriptionManagerTasks[]{client2tasks, client1tasks}) {
			if (smt != null) setupClient(smt, serverCaCertFile, generatedProductCertFiles);
		}
		
		// initialize various servertasks instance variables for future reference
		servertasks.initialize(clienttasks.candlepinAdminUsername, clienttasks.candlepinAdminPassword, clienttasks.candlepinUrl);
		
		// create an artifact to log all the package versions being tested
		File file = new File("test-output/version.txt"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
		Writer output = new BufferedWriter(new FileWriter(file));
		String infoMsg;
		if (client1 != null) {
			infoMsg = "Client1 System Hostname: "+sm_client1Hostname+"\n";
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "Beaker Compose: "+client1tasks.compose+"\n";
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "Installed Packages:";
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client1.runCommandAndWait("rpm -qa | egrep subscription-manager").getStdout();	// subscription-manager-0.63-1.el6.i686
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client1.runCommandAndWait("rpm -qa | egrep ^rhsm-gtk").getStdout();	// subscription-manager-0.63-1.el6.i686
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client1.runCommandAndWait("rpm -qa | egrep intentctl").getStdout();	// subscription-manager-0.63-1.el6.i686
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client1.runCommandAndWait("rpm -qa | egrep ^python-rhsm").getStdout();	// python-rhsm-0.63-1.el6.i686
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client1.runCommandAndWait("rpm -q --whatprovides /etc/redhat-release").getStdout();
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "# cat /etc/redhat-release";
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client1.runCommandAndWait("cat /etc/redhat-release").getStdout();	// Red Hat Enterprise Linux Server release 6.1 Beta (Santiago)
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "# uname -a";
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client1.runCommandAndWait("uname -a").getStdout();	// Linux jsefler-onprem-server.usersys.redhat.com 2.6.32-122.el6.x86_64 #1 SMP Wed Mar 9 23:54:34 EST 2011 x86_64 x86_64 x86_64 GNU/Linux
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "# "+client1tasks.listCommand(null, null, null, true, null, null, null, null, null, null, null, null, null, null, null);
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client1.runCommandAndWait(client1tasks.listCommand(null, null, null, true, null, null, null, null, null, null, null, null, null, null, null)).getStdout();
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			for (ProductCert productCert : client1tasks.getCurrentProductCerts()) {
				infoMsg = "# rpm -q --whatprovides "+productCert.file;
				log.info(infoMsg); output.write(infoMsg+"\n");
				infoMsg = client1.runCommandAndWait("rpm -q --whatprovides "+productCert.file).getStdout();
				log.info(infoMsg); output.write(infoMsg+"\n\n");
			}
			
			infoMsg = "# cat "+client1tasks.productIdJsonFile;
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client1.runCommandAndWait("cat "+client1tasks.productIdJsonFile).getStdout();
			log.info(infoMsg); output.write(infoMsg+"\n\n");
		}
		if (client2 != null) {
			infoMsg = "Client2 System Hostname: "+sm_client2Hostname+"\n";
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "Beaker Compose: "+client2tasks.compose+"\n";
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "Installed Packages:";
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client2.runCommandAndWait("rpm -qa | egrep subscription-manager").getStdout();	// subscription-manager-0.63-1.el6.i686
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client2.runCommandAndWait("rpm -qa | egrep ^rhsm-gtk").getStdout();	// subscription-manager-0.63-1.el6.i686
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client2.runCommandAndWait("rpm -qa | egrep intentctl").getStdout();	// subscription-manager-0.63-1.el6.i686
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client2.runCommandAndWait("rpm -qa | egrep ^python-rhsm").getStdout();	// python-rhsm-0.63-1.el6.i686
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client2.runCommandAndWait("rpm -q --whatprovides /etc/redhat-release").getStdout();
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "# cat /etc/redhat-release";
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client2.runCommandAndWait("cat /etc/redhat-release").getStdout();	// Red Hat Enterprise Linux Server release 6.1 Beta (Santiago)
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "# uname -a";
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client2.runCommandAndWait("uname -a").getStdout();	// Linux jsefler-onprem-server.usersys.redhat.com 2.6.32-122.el6.x86_64 #1 SMP Wed Mar 9 23:54:34 EST 2011 x86_64 x86_64 x86_64 GNU/Linux
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			infoMsg = "# "+client2tasks.listCommand(null, null, null, true, null, null, null, null, null, null, null, null, null, null, null);
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client2.runCommandAndWait(client2tasks.listCommand(null, null, null, true, null, null, null, null, null, null, null, null, null, null, null)).getStdout();
			log.info(infoMsg); output.write(infoMsg+"\n");
			
			for (ProductCert productCert : client2tasks.getCurrentProductCerts()) {
				infoMsg = "# rpm -q --whatprovides "+productCert.file;
				log.info(infoMsg); output.write(infoMsg+"\n");
				infoMsg = client2.runCommandAndWait("rpm -q --whatprovides "+productCert.file).getStdout();
				log.info(infoMsg); output.write(infoMsg+"\n\n");
			}
			
			infoMsg = "# cat "+client2tasks.productIdJsonFile;
			log.info(infoMsg); output.write(infoMsg+"\n");
			infoMsg = client2.runCommandAndWait("cat "+client2tasks.productIdJsonFile).getStdout();
			log.info(infoMsg); output.write(infoMsg+"\n\n");
		}
		output.close();
		
		// create an artifact containing package versions that can be uploaded to Polarion Group ID 
		String groupId = "";
		if (clienttasks.isPackageVersion("subscription-manager",">=","0")!=null) {
			groupId += " "+clienttasks.installedPackageVersionMap.get("subscription-manager");
		}
		/* exclude for now since the Polarion Group ID should really be a multi-entry field
		 * RHEL Projects: Use of Group ID and Component for Test Runs
		 * https://projects.engineering.redhat.com/browse/POLARION-1201
		if (clienttasks.isPackageVersion("python-rhsm",">=","0")!=null) {
			groupId += " "+clienttasks.installedPackageVersionMap.get("python-rhsm");
		}
		if (clienttasks.isPackageVersion("subscription-manager-migration-data",">=","0")!=null) {
			groupId += " "+clienttasks.installedPackageVersionMap.get("subscription-manager-migration-data");
		}
		*/
		groupId = groupId.replaceAll("\\.el"+clienttasks.redhatReleaseX, "");	// strip off .el5
		groupId = groupId.replaceAll("\\."+clienttasks.arch+"|\\.noarch", "");	// strip off .arch
		groupId = groupId.trim();
		/*File*/ file = new File("test-output/group-id.txt"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
		/*Writer*/ output = new BufferedWriter(new FileWriter(file));
		output.write(groupId);
		output.close();
		
		
		isSetupBeforeSuiteComplete = true;
	}
	
	/**
	 * @return the candlepin CA Cert (only when the candlepin server is not hosted) otherwise return null
	 * @throws IOException
	 */
	protected File fetchServerCaCertFile () throws IOException {
		File serverCaCertFile=null;
		if (server!=null && !sm_serverType.equals(CandlepinType.hosted)) {
			log.info("Fetching Candlepin CA cert...");
			serverCaCertFile = new File((getProperty("automation.dir", "/tmp")+"/tmp/"+servertasks.candlepinCACertFile.getName()).replace("tmp/tmp", "tmp"));
			RemoteFileTasks.getFile(server, serverCaCertFile.getParent(), servertasks.candlepinCACertFile.getPath());
		}
		return serverCaCertFile;
	}
	
	/**
	 * @param ciMessage - the value of an environment variable called CI_MESSAGE
	 *  set by the Jenkins ci-trigger plugin
	 * @return List of urls that can be used to update packages that are already
	 *  installed on the system.  If a package from the ciMessage is not already
	 *  installed on the system, then it will not be included in the return list.
	 * @throws JSONException
	 */
	protected List<String> getRpmUpdateUrlsFromCiMessage(String ciMessage) throws JSONException {
		List<String> rpmUpdateUrls = new ArrayList<String>();
		if (ciMessage==null || ciMessage.isEmpty()) return rpmUpdateUrls;

		// Example CI_MESSAGE:
		
		//	{
		//	  "tag" : {
		//	    "maven_support" : false,
		//	    "locked" : false,
		//	    "name" : "rhel-7.2-candidate",
		//	    "perm" : null,
		//	    "perm_id" : null,
		//	    "arches" : null,
		//	    "maven_include_all" : false,
		//	    "id" : 7604
		//	  },
		//	  "force" : false,
		//	  "build" : {
		//	    "owner_name" : "crog",
		//	    "package_name" : "python-rhsm",
		//	    "task_id" : 9402045,
		//	    "volume_name" : "DEFAULT",
		//	    "owner_id" : 3046,
		//	    "creation_event_id" : 11465239,
		//	    "creation_time" : "2015-06-22 14:58:31.692135",
		//	    "state" : 1,
		//	    "nvr" : "python-rhsm-1.15.2-1.el7",
		//	    "completion_time" : "2015-06-22 15:46:14.77846",
		//	    "epoch" : null,
		//	    "version" : "1.15.2",
		//	    "creation_ts" : 1.43499951169213E9,
		//	    "volume_id" : 0,
		//	    "release" : "1.el7",
		//	    "package_id" : 30891,
		//	    "completion_ts" : 1.43500237477846E9,
		//	    "id" : 443277,
		//	    "name" : "python-rhsm"
		//	  },
		//	  "user" : {
		//	    "status" : 0,
		//	    "usertype" : 0,
		//	    "krb_principal" : "crog@REDHAT.COM",
		//	    "id" : 3046,
		//	    "name" : "crog"
		//	  },
		//	  "rpms" : {
		//	    "s390x" : [ "python-rhsm-1.15.2-1.el7.s390x.rpm", "python-rhsm-debuginfo-1.15.2-1.el7.s390x.rpm" ],
		//	    "s390" : [ "python-rhsm-1.15.2-1.el7.s390.rpm", "python-rhsm-debuginfo-1.15.2-1.el7.s390.rpm" ],
		//	    "i686" : [ "python-rhsm-1.15.2-1.el7.i686.rpm", "python-rhsm-debuginfo-1.15.2-1.el7.i686.rpm" ],
		//	    "ppc64" : [ "python-rhsm-1.15.2-1.el7.ppc64.rpm", "python-rhsm-debuginfo-1.15.2-1.el7.ppc64.rpm" ],
		//	    "aarch64" : [ "python-rhsm-1.15.2-1.el7.aarch64.rpm", "python-rhsm-debuginfo-1.15.2-1.el7.aarch64.rpm" ],
		//	    "ppc64le" : [ "python-rhsm-1.15.2-1.el7.ppc64le.rpm", "python-rhsm-debuginfo-1.15.2-1.el7.ppc64le.rpm" ],
		//	    "x86_64" : [ "python-rhsm-1.15.2-1.el7.x86_64.rpm", "python-rhsm-debuginfo-1.15.2-1.el7.x86_64.rpm" ],
		//	    "ppc" : [ "python-rhsm-1.15.2-1.el7.ppc.rpm", "python-rhsm-debuginfo-1.15.2-1.el7.ppc.rpm" ],
		//	    "src" : [ "python-rhsm-1.15.2-1.el7.src.rpm" ]
		//	  },
		//	  "tags" : [ "rhel-7.2-candidate" ],
		//	  "archives" : { }
		//	}
		
		//	{
		//	  "tag" : {
		//	    "maven_support" : false,
		//	    "locked" : false,
		//	    "name" : "rhel-7.2-candidate",
		//	    "perm" : null,
		//	    "perm_id" : null,
		//	    "arches" : null,
		//	    "maven_include_all" : false,
		//	    "id" : 7604
		//	  },
		//	  "force" : false,
		//	  "build" : {
		//	    "owner_name" : "crog",
		//	    "package_name" : "subscription-manager",
		//	    "task_id" : 9402040,
		//	    "volume_name" : "DEFAULT",
		//	    "owner_id" : 3046,
		//	    "creation_event_id" : 11465228,
		//	    "creation_time" : "2015-06-22 14:56:44.401754",
		//	    "state" : 1,
		//	    "nvr" : "subscription-manager-1.15.2-1.el7",
		//	    "completion_time" : "2015-06-22 15:46:58.002083",
		//	    "epoch" : null,
		//	    "version" : "1.15.2",
		//	    "creation_ts" : 1.43499940440175E9,
		//	    "volume_id" : 0,
		//	    "release" : "1.el7",
		//	    "package_id" : 19375,
		//	    "completion_ts" : 1.43500241800208E9,
		//	    "id" : 443276,
		//	    "name" : "subscription-manager"
		//	  },
		//	  "user" : {
		//	    "status" : 0,
		//	    "usertype" : 0,
		//	    "krb_principal" : "crog@REDHAT.COM",
		//	    "id" : 3046,
		//	    "name" : "crog"
		//	  },
		//	  "rpms" : {
		//	    "s390x" : [ "subscription-manager-1.15.2-1.el7.s390x.rpm", "subscription-manager-debuginfo-1.15.2-1.el7.s390x.rpm", "subscription-manager-firstboot-1.15.2-1.el7.s390x.rpm", "subscription-manager-gui-1.15.2-1.el7.s390x.rpm", "subscription-manager-migration-1.15.2-1.el7.s390x.rpm", "subscription-manager-plugin-container-1.15.2-1.el7.s390x.rpm", "subscription-manager-plugin-ostree-1.15.2-1.el7.s390x.rpm" ],
		//	    "s390" : [ "subscription-manager-1.15.2-1.el7.s390.rpm", "subscription-manager-debuginfo-1.15.2-1.el7.s390.rpm", "subscription-manager-firstboot-1.15.2-1.el7.s390.rpm", "subscription-manager-gui-1.15.2-1.el7.s390.rpm", "subscription-manager-migration-1.15.2-1.el7.s390.rpm", "subscription-manager-plugin-container-1.15.2-1.el7.s390.rpm", "subscription-manager-plugin-ostree-1.15.2-1.el7.s390.rpm" ],
		//	    "i686" : [ "subscription-manager-1.15.2-1.el7.i686.rpm", "subscription-manager-debuginfo-1.15.2-1.el7.i686.rpm", "subscription-manager-firstboot-1.15.2-1.el7.i686.rpm", "subscription-manager-gui-1.15.2-1.el7.i686.rpm", "subscription-manager-migration-1.15.2-1.el7.i686.rpm", "subscription-manager-plugin-container-1.15.2-1.el7.i686.rpm", "subscription-manager-plugin-ostree-1.15.2-1.el7.i686.rpm" ],
		//	    "ppc64" : [ "subscription-manager-1.15.2-1.el7.ppc64.rpm", "subscription-manager-debuginfo-1.15.2-1.el7.ppc64.rpm", "subscription-manager-firstboot-1.15.2-1.el7.ppc64.rpm", "subscription-manager-gui-1.15.2-1.el7.ppc64.rpm", "subscription-manager-migration-1.15.2-1.el7.ppc64.rpm", "subscription-manager-plugin-container-1.15.2-1.el7.ppc64.rpm", "subscription-manager-plugin-ostree-1.15.2-1.el7.ppc64.rpm" ],
		//	    "aarch64" : [ "subscription-manager-1.15.2-1.el7.aarch64.rpm", "subscription-manager-debuginfo-1.15.2-1.el7.aarch64.rpm", "subscription-manager-firstboot-1.15.2-1.el7.aarch64.rpm", "subscription-manager-gui-1.15.2-1.el7.aarch64.rpm", "subscription-manager-migration-1.15.2-1.el7.aarch64.rpm", "subscription-manager-plugin-container-1.15.2-1.el7.aarch64.rpm", "subscription-manager-plugin-ostree-1.15.2-1.el7.aarch64.rpm" ],
		//	    "ppc64le" : [ "subscription-manager-1.15.2-1.el7.ppc64le.rpm", "subscription-manager-debuginfo-1.15.2-1.el7.ppc64le.rpm", "subscription-manager-firstboot-1.15.2-1.el7.ppc64le.rpm", "subscription-manager-gui-1.15.2-1.el7.ppc64le.rpm", "subscription-manager-migration-1.15.2-1.el7.ppc64le.rpm", "subscription-manager-plugin-container-1.15.2-1.el7.ppc64le.rpm", "subscription-manager-plugin-ostree-1.15.2-1.el7.ppc64le.rpm" ],
		//	    "x86_64" : [ "subscription-manager-1.15.2-1.el7.x86_64.rpm", "subscription-manager-debuginfo-1.15.2-1.el7.x86_64.rpm", "subscription-manager-firstboot-1.15.2-1.el7.x86_64.rpm", "subscription-manager-gui-1.15.2-1.el7.x86_64.rpm", "subscription-manager-migration-1.15.2-1.el7.x86_64.rpm", "subscription-manager-plugin-container-1.15.2-1.el7.x86_64.rpm", "subscription-manager-plugin-ostree-1.15.2-1.el7.x86_64.rpm" ],
		//	    "ppc" : [ "subscription-manager-1.15.2-1.el7.ppc.rpm", "subscription-manager-debuginfo-1.15.2-1.el7.ppc.rpm", "subscription-manager-firstboot-1.15.2-1.el7.ppc.rpm", "subscription-manager-gui-1.15.2-1.el7.ppc.rpm", "subscription-manager-migration-1.15.2-1.el7.ppc.rpm", "subscription-manager-plugin-container-1.15.2-1.el7.ppc.rpm", "subscription-manager-plugin-ostree-1.15.2-1.el7.ppc.rpm" ],
		//	    "src" : [ "subscription-manager-1.15.2-1.el7.src.rpm" ]
		//	  },
		//	  "tags" : [ "rhel-7.2-candidate" ],
		//	  "archives" : { }
		//	}
		
		//	{
		//	  "tag" : {
		//	    "maven_support" : false,
		//	    "locked" : false,
		//	    "name" : "rhel-7.2-candidate",
		//	    "perm" : null,
		//	    "perm_id" : null,
		//	    "arches" : null,
		//	    "maven_include_all" : false,
		//	    "id" : 7604
		//	  },
		//	  "force" : false,
		//	  "build" : {
		//	    "owner_name" : "awood",
		//	    "package_name" : "subscription-manager-migration-data",
		//	    "task_id" : 9421528,
		//	    "volume_name" : "DEFAULT",
		//	    "owner_id" : 788,
		//	    "creation_event_id" : 11486530,
		//	    "creation_time" : "2015-06-25 09:50:44.416348",
		//	    "state" : 1,
		//	    "nvr" : "subscription-manager-migration-data-2.0.22-1.el7",
		//	    "completion_time" : "2015-06-25 09:53:54.392923",
		//	    "epoch" : null,
		//	    "version" : "2.0.22",
		//	    "creation_ts" : 1.43524024441635E9,
		//	    "volume_id" : 0,
		//	    "release" : "1.el7",
		//	    "package_id" : 34209,
		//	    "completion_ts" : 1.43524043439292E9,
		//	    "id" : 444026,
		//	    "name" : "subscription-manager-migration-data"
		//	  },
		//	  "user" : {
		//	    "status" : 0,
		//	    "usertype" : 0,
		//	    "krb_principal" : "awood@REDHAT.COM",
		//	    "id" : 788,
		//	    "name" : "awood"
		//	  },
		//	  "rpms" : {
		//	    "noarch" : [ "subscription-manager-migration-data-2.0.22-1.el7.noarch.rpm" ],
		//	    "src" : [ "subscription-manager-migration-data-2.0.22-1.el7.src.rpm" ]
		//	  },
		//	  "tags" : [ "rhel-7.2-candidate" ],
		//	  "archives" : { }
		//	}
	
		// parse the sm_ciMessage into additional sm_rpmUpdateUrls
		JSONObject jsonCIMessage = new JSONObject(ciMessage);
		String arch = clienttasks.arch;	// assume
		if (jsonCIMessage.getJSONObject("rpms").has("noarch")) arch="noarch";
		JSONArray jsonCIMessageRpms = jsonCIMessage.getJSONObject("rpms").getJSONArray(arch);
		List<String> ciMessageRpms = new ArrayList<String>();
		for (int r = 0; r < jsonCIMessageRpms.length(); r++) ciMessageRpms.add(jsonCIMessageRpms.getString(r));
		String jsonCIMessageBuildName    = jsonCIMessage.getJSONObject("build").getString("name");	// python-rhsm
		String jsonCIMessageBuildVersion = jsonCIMessage.getJSONObject("build").getString("version");	// 1.15.2
		String jsonCIMessageBuildRelease = jsonCIMessage.getJSONObject("build").getString("release");	// 1.el7
		for (String rpm : ciMessageRpms) {
			String pkgVersionReleaseArch = rpm.replaceFirst(".rpm$", "");	// python-rhsm-1.15.2-1.el7.x86_64
			String pkg = pkgVersionReleaseArch.split("-"+jsonCIMessageBuildVersion+"-"+jsonCIMessageBuildRelease)[0];	// python-rhsm or python-rhsm-debuginfo
			
			String rpmUpdateUrl = "http://download.devel.redhat.com/brewroot/packages/"+jsonCIMessageBuildName+"/"+jsonCIMessageBuildVersion+"/"+jsonCIMessageBuildRelease+"/"+arch+"/"+rpm;
			if (clienttasks.isPackageInstalled(pkg)) {
				rpmUpdateUrls.add(rpmUpdateUrl);
			} else {
				log.warning("Package '"+pkg+"' is not already installed and will NOT be updated to '"+pkgVersionReleaseArch+"'.");
			}
		}
		return rpmUpdateUrls;
	}
	
	public void setupClient(SubscriptionManagerTasks smt, File serverCaCertFile, List<File> generatedProductCertFiles) throws IOException, JSONException{		
		
		String yumInstallOptionsToEnableLatestAppStream = "";
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=8) {
			yumInstallOptionsToEnableLatestAppStream += " --enablerepo="+clienttasks.configureLatestAppStreamRepo();
		}
		
		smt.installSubscriptionManagerRPMs(sm_yumInstallOptions+yumInstallOptionsToEnableLatestAppStream);
		if (sm_yumInstallZStreamUpdates)	{
			smt.installZStreamUpdates(sm_yumInstallOptions+yumInstallOptionsToEnableLatestAppStream, sm_yumInstallZStreamUpdatePackages, sm_yumInstallZStreamComposeUrl, sm_yumInstallZStreamBrewUrl, sm_ciMessage);
		}
		sm_rpmUpdateUrls.addAll(getRpmUpdateUrlsFromCiMessage(sm_ciMessage));
		smt.installSubscriptionManagerRPMs(sm_rpmInstallUrls,sm_rpmUpdateUrls,sm_yumInstallOptions, jenkinsUsername,jenkinsPassword);
		smt.initializeMsgStringsAfterInstallingSubscriptionManagerRPMs();
		
		// rewrite rhsmcertd.certFrequency -> rhsmcertd.certCheckInterval   see bug 882459
		String certFrequency = smt.getConfFileParameter(smt.rhsmConfFile, "rhsmcertd", "certFrequency");
		if (certFrequency!=null) {
			smt.commentConfFileParameter(smt.rhsmConfFile, "certFrequency");
			//smt.config(null, null, true, new String[]{"rhsmcertd","certCheckInterval".toLowerCase(),certFrequency});
			smt.addConfFileParameter(smt.rhsmConfFile, "rhsmcertd", "certCheckInterval", certFrequency);
		}
		// rewrite rhsmcertd.healFrequency -> rhsmcertd.autoAttachInterval   see bug 882459
		String healFrequency = smt.getConfFileParameter(smt.rhsmConfFile, "rhsmcertd", "healFrequency");
		if (healFrequency!=null) {
			smt.commentConfFileParameter(smt.rhsmConfFile, "healFrequency");
			//smt.config(null, null, true, new String[]{"rhsmcertd","autoAttachInterval".toLowerCase(),healFrequency});
			smt.addConfFileParameter(smt.rhsmConfFile, "rhsmcertd", "autoAttachInterval", healFrequency);
		}
		
		// rhsm.conf [server] configurations
		if (!sm_serverHostname.equals(""))				smt.updateConfFileParameter(smt.rhsmConfFile, "hostname", sm_serverHostname);							else sm_serverHostname = smt.getConfFileParameter(smt.rhsmConfFile, "hostname");
		if (!sm_serverPrefix.equals(""))				smt.updateConfFileParameter(smt.rhsmConfFile, "prefix", sm_serverPrefix);								else sm_serverPrefix = smt.getConfFileParameter(smt.rhsmConfFile, "prefix");
		if (!sm_serverPort.equals(""))					smt.updateConfFileParameter(smt.rhsmConfFile, "port", sm_serverPort);									else sm_serverPort = smt.getConfFileParameter(smt.rhsmConfFile, "port");
		if (!sm_serverInsecure.equals(""))				smt.updateConfFileParameter(smt.rhsmConfFile, "insecure", sm_serverInsecure);							else sm_serverInsecure = smt.getConfFileParameter(smt.rhsmConfFile, "insecure");
		if (!sm_serverSslVerifyDepth.equals(""))		smt.updateConfFileParameter(smt.rhsmConfFile, "ssl_verify_depth", sm_serverSslVerifyDepth);							else sm_serverInsecure = smt.getConfFileParameter(smt.rhsmConfFile, "insecure");
		if (!sm_serverCaCertDir.equals(""))				smt.updateConfFileParameter(smt.rhsmConfFile, "ca_cert_dir", sm_serverCaCertDir);						else sm_serverCaCertDir = smt.getConfFileParameter(smt.rhsmConfFile, "ca_cert_dir");
	
		// rhsm.conf [rhsm] configurations
		if (!sm_rhsmBaseUrl.equals(""))					smt.updateConfFileParameter(smt.rhsmConfFile, "baseurl", sm_rhsmBaseUrl);								else sm_rhsmBaseUrl = smt.getConfFileParameter(smt.rhsmConfFile, "baseurl");
		if (!sm_rhsmRepoCaCert.equals(""))				smt.updateConfFileParameter(smt.rhsmConfFile, "repo_ca_cert", sm_rhsmRepoCaCert);						else sm_rhsmRepoCaCert = smt.getConfFileParameter(smt.rhsmConfFile, "repo_ca_cert");
		//if (!rhsmShowIncompatiblePools.equals(""))	smt.updateConfFileParameter(smt.rhsmConfFile, "showIncompatiblePools", rhsmShowIncompatiblePools);		else rhsmShowIncompatiblePools = smt.getConfFileParameter(smt.rhsmConfFile, "showIncompatiblePools");
		if (!sm_rhsmProductCertDir.equals(""))			smt.updateConfFileParameter(smt.rhsmConfFile, "productCertDir", sm_rhsmProductCertDir);					else sm_rhsmProductCertDir = smt.getConfFileParameter(smt.rhsmConfFile, "productCertDir");
		if (!sm_rhsmEntitlementCertDir.equals(""))		smt.updateConfFileParameter(smt.rhsmConfFile, "entitlementCertDir", sm_rhsmEntitlementCertDir);			else sm_rhsmEntitlementCertDir = smt.getConfFileParameter(smt.rhsmConfFile, "entitlementCertDir");
		if (!sm_rhsmConsumerCertDir.equals(""))			smt.updateConfFileParameter(smt.rhsmConfFile, "consumerCertDir", sm_rhsmConsumerCertDir);				else sm_rhsmConsumerCertDir = smt.getConfFileParameter(smt.rhsmConfFile, "consumerCertDir");
	
		// rhsm.conf [rhsmcertd] configurations
		if (!sm_rhsmcertdCertFrequency.equals(""))		smt.updateConfFileParameter(smt.rhsmConfFile, /*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval",	sm_rhsmcertdCertFrequency);				else sm_rhsmcertdCertFrequency = smt.getConfFileParameter(smt.rhsmConfFile, /*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval");
		if (!sm_rhsmcertdHealFrequency.equals(""))		smt.updateConfFileParameter(smt.rhsmConfFile, /*"healFrequency" CHANGED BY BUG 882459 TO*/"autoAttachInterval",	sm_rhsmcertdHealFrequency);				else sm_rhsmcertdHealFrequency = smt.getConfFileParameter(smt.rhsmConfFile, /*"healFrequency" CHANGED BY BUG 882459 TO*/"autoAttachInterval");
		
		smt.initializeFieldsFromConfigFile();
		smt.removeAllCerts(true,true, false);
		smt.removeAllFacts();
		smt.initializeSystemComplianceAttributes();
		smt.removeRhnSystemIdFile();
		smt.installRepoCaCerts(sm_repoCaCertUrls);
		smt.restartCockpitServices();
		smt.setupRhnDefinitions(sm_rhnDefinitionsGitRepository);
		smt.setupTranslateToolkitFromTarUrl(sm_translateToolkitTarUrl);
		
		// create a facts file that will tell candlepin what version of x509 entitlement certificates this client understands;  removeAllFacts() should be called before this block of code!
		if (sm_clientCertificateVersion!=null) {
			Map<String,String> map = new HashMap<String,String>();
			map.put("system.certificate_version",sm_clientCertificateVersion);
			smt.createFactsFileWithOverridingValues(smt.certVersionFactsFilename, map);
		}

		// transfer a copy of the candlepin CA Cert from the candlepin server to the clients so we can test in secure mode
		log.info("Copying Candlepin cert onto client to enable certificate validation...");
		smt.installRepoCaCert(serverCaCertFile, sm_serverHostname.split("\\.")[0]+".pem");
		
		// transfer copies of all the generated product certs from the candlepin server to the clients
		log.info("Copying Candlepin generated product certs onto client to simulate installed products...");
		smt.installProductCerts(generatedProductCertFiles);
		
		// initialize variables used to access CandlepinTasks from SubcriptionManagerTasks
		smt.candlepinAdminUsername = sm_serverAdminUsername;
		smt.candlepinAdminPassword = sm_serverAdminPassword;
		smt.candlepinUrl = sm_serverUrl.isEmpty() ? getServerUrl(smt.getConfFileParameter(smt.rhsmConfFile,"hostname"), smt.getConfFileParameter(smt.rhsmConfFile,"port"), smt.getConfFileParameter(smt.rhsmConfFile,"prefix")) : sm_serverUrl;
		sm_serverUrl = smt.candlepinUrl;	// rewrite it back to sm_serverUrl to make it easier for tests to access
		
		// set the rhsm.log logging level to DEBUG - effectively reverting the changes from Bug 1266935 - Reduce default log level to INFO (from DEBUG)
		// this is needed to enable more testing and log more useful info from SubscriptionManagerTasks.logRuntimeErrors(...)
		if (smt.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) {// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
			smt.updateConfFileParameter(clienttasks.rhsmConfFile, "logging", "default_log_level", "DEBUG");
			//smt.config(null, null, true, new String[]{"logging","default_log_level","DEBUG"});
		} else
		if (smt.isPackageVersion("subscription-manager", ">=", "1.14.2-1")) {// commit 66aafd77dc629b921379f0e121421c1c21c0b787 Move to fileConfig based logging.
			smt.updateConfFileParameter(clienttasks.rhsmLoggingConfFile, "handler_rhsm_log", "level", "DEBUG");
		}
		
		// TEMPORARY WORKAROUND
		if (CandlepinType.hosted.equals(sm_serverType)) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1358508";	// Bug 1358508 - Error updating system data on the server
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				//smt.updateConfFileParameter(smt.rhsmConfFile, "rhsm", "report_package_profile", "0");
				smt.config(null, null, true, new String[]{"rhsm", "report_package_profile", "0"});
			}
		}
		// END OF WORKAROUND
		
		// if this client came from a snapshot compose, it might not have an installed product cert
		// let's manually place a reasonable product cert based on the polarion.planned_in property
		// Reference: https://mojo.redhat.com/docs/DOC-1157988
 		if (smt.getCurrentProductCerts().isEmpty()) {
			if (polarionPlannedIn.toLowerCase().contains("Snap".toLowerCase())) {
				List<File> productCertFiles = new ArrayList<File>();
				if (smt.compose.startsWith("RHEL-ALT")) {	// RHEL-ALT compose
					if (smt.variant.equals("Server") && smt.arch.equals("aarch64"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-alt-%s-beta/%s-%s-363.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux for ARM 64 Beta
					if (smt.variant.equals("Server") && smt.arch.equals("ppc64le"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-alt-%s-beta/%s-%s-362.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux for Power 9 Beta
					if (smt.variant.equals("Server") && smt.arch.equals("s390x"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-alt-%s-beta/%s-%s-433.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux for IBM System z (Structure A) Beta
				} else if (smt.compose.startsWith("RHEL-")){	// RHEL compose
					if (smt.variant.equals("Server") && smt.arch.equals("aarch64"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-294.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux Server for ARM
					if (smt.variant.equals("Server") && smt.arch.equals("ppc64le"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-279.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux for Power, little endian
					if (smt.variant.equals("Server") && smt.arch.equals("ppc64"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-74.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux for Power, big endian
					if (smt.variant.equals("Server") && smt.arch.equals("s390x"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-72.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux for IBM z Systems
//HTB				if (smt.variant.equals("Server") && smt.arch.equals("x86_64"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-69.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux Server
					if (smt.variant.equals("Server") && smt.arch.equals("i386"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-69.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux Server
					if (smt.variant.equals("Server") && smt.arch.equals("i686"))		productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-69.pem",smt.redhatReleaseXY,smt.variant,"i386")));	// Red Hat Enterprise Linux Server
					if (smt.variant.equals("Client"))									productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-68.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux Desktop
					if (smt.variant.equals("ComputeNode"))								productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-76.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux for Scientific Computing
//HTB				if (smt.variant.equals("Workstation") && smt.arch.equals("x86_64"))	productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-71.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux Workstation
					if (smt.variant.equals("Workstation") && smt.arch.equals("i386"))	productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-71.pem",smt.redhatReleaseXY,smt.variant,smt.arch)));	// Red Hat Enterprise Linux Workstation
					if (smt.variant.equals("Workstation") && smt.arch.equals("i686"))	productCertFiles.add(new File(getProperty("automation.dir", "/tmp")+String.format("/certs/rhel-%s-beta/%s-%s-71.pem",smt.redhatReleaseXY,smt.variant,"i386")));	// Red Hat Enterprise Linux Workstation
				}
				log.info("Manually placing a RHEL product cert onto the system to enable more testing...");
				smt.installProductCerts(productCertFiles);	
			}
		}
	}
	
	protected static boolean isSetupBeforeSuiteComplete = false;
	
//	@BeforeSuite(groups={"gui-setup"},dependsOnMethods={"setupBeforeSuite"}, description="subscription manager gui set up")
//	public void setupGUIBeforeSuite() throws IOException {
//		// 201104251443:55.877 - FINE: ssh root@jsefler-onprem-workstation.usersys.redhat.com service vncserver restart (com.redhat.qe.tools.SSHCommandRunner.run)
//		// 201104251444:02.676 - FINE: Stdout: 
//		// Shutting down VNC server: 2:root [  OK  ]
//		// Starting VNC server: 2:root [  OK  ]
//		//if (client1!=null) RemoteFileTasks.runCommandAndAssert(client1, "service vncserver restart", /*Integer.valueOf(0) DON"T CHECK EXIT CODE SINCE IT RETURNS 1 WHEN STOP FAILS EVEN THOUGH START SUCCEEDS*/null, "Starting VNC server: 2:root \\[  OK  \\]", null);
//		//if (client2!=null) RemoteFileTasks.runCommandAndAssert(client2, "service vncserver restart", /*Integer.valueOf(0) DON"T CHECK EXIT CODE SINCE IT RETURNS 1 WHEN STOP FAILS EVEN THOUGH START SUCCEEDS*/null, "Starting VNC server: 2:root \\[  OK  \\]", null);
//		if (client1!=null) client1.runCommandAndWait("service vncserver restart");
//		if (client2!=null) client2.runCommandAndWait("service vncserver restart");
//
//		// vncviewer <client1tasks.hostname>:2
//	}
	
	
	protected String selinuxSuiteMarker = "SM TestSuite marker";	// do not use a timestamp on the whole suite marker
	protected String selinuxClassMarker = "SM TestClass marker "+String.valueOf(System.currentTimeMillis());	// using a timestamp on the class marker will help identify the test class during which a denial is logged
	@BeforeSuite(groups={"setup"},dependsOnMethods={"setupBeforeSuite"}, description="Ensure SELinux is Enforcing before running the test suite.")
	public void ensureSELinuxIsEnforcingBeforeSuite() {
		for (SubscriptionManagerTasks clienttasks : Arrays.asList(client1tasks,client2tasks)) {
			if (clienttasks!=null) {
				
				// mark the audit.log file so it can be asserted for denials in verifyNoSELinuxDenialsWereLoggedAfterClass()
				RemoteFileTasks.markFile(clienttasks.sshCommandRunner, clienttasks.auditLogFile, selinuxSuiteMarker);
				
				// TEMPORARY WORKAROUND
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1257940";	// Bug 1257940 - systemd-hwdb should be confined 
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Skipping BeforeSuite assertion that selinux is Enforcing while bug "+bugId+" is open.");
					continue;
				}
				// END OF WORKAROUND
				// TEMPORARY WORKAROUND
				invokeWorkaroundWhileBugIsOpen = true;
				bugId="1342401";	// Bug 1342401 - Allow NetworkManager to create temporary /etc/resolv.conf.XXXXXX file 
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Skipping BeforeSuite assertion that selinux is Enforcing while bug "+bugId+" is open.");
					continue;
				}
				// END OF WORKAROUND
				// TEMPORARY WORKAROUND
				invokeWorkaroundWhileBugIsOpen = true;
				bugId="1343648";	// Bug 1343648 - SELinux label for /etc/udev/hwdb.bin is etc_t instead of systemd_hwdb_etc_t after "#systemd-hwdb update" 
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Skipping BeforeSuite assertion that selinux is Enforcing while bug "+bugId+" is open.");
					continue;
				}
				// END OF WORKAROUND
				// TEMPORARY WORKAROUND
				invokeWorkaroundWhileBugIsOpen = true;
				bugId="1350756";	// Bug 1350756 - SELinux label for /etc/udev/hwdb.bin is etc_t instead of systemd_hwdb_etc_t after "#systemd-hwdb update" 
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Skipping BeforeSuite assertion that selinux is Enforcing while bug "+bugId+" is open.");
					continue;
				}
				// END OF WORKAROUND
				
				// assert selinux is Enforcing
				Assert.assertEquals(clienttasks.sshCommandRunner.runCommandAndWait("getenforce").getStdout().trim(), "Enforcing", "SELinux mode is set to enforcing on client "+clienttasks.sshCommandRunner.getConnection().getRemoteHostname());
			}
		}
	}
	
	@BeforeClass(groups={"setup"}, description="Mark the SELinux audit log before running the current class of tests so it can be searched for denials after the test class has run.")
	public void MarkSELinuxAuditLogBeforeClass() {
		for (SubscriptionManagerTasks clienttasks : Arrays.asList(client1tasks,client2tasks)) {
			if (clienttasks!=null) {
				RemoteFileTasks.markFile(clienttasks.sshCommandRunner, clienttasks.auditLogFile, selinuxClassMarker);
			}
		}
	}
	@AfterClass(groups={"setup"}, description="Search the SELinux audit log for denials after running the current class of tests")
	public void verifyNoSELinuxDenialsWereLoggedAfterClass() {
		for (SubscriptionManagerTasks clienttasks : Arrays.asList(client1tasks,client2tasks)) {
			if (clienttasks!=null) {
				String avcRegex;
				String tailFromMarkedFile = RemoteFileTasks.getTailFromMarkedFile(clienttasks.sshCommandRunner, clienttasks.auditLogFile, selinuxClassMarker, "denied").trim();
				
				// TEMPORARY WORKAROUND
				// [root@jsefler-rhel7 ~]# tail -f /var/log/audit/audit.log | grep AVC
				// type=USER_AVC msg=audit(1470087122.008:24063): pid=693 uid=81 auid=4294967295 ses=4294967295 subj=system_u:system_r:system_dbusd_t:s0-s0:c0.c1023 msg='avc:  denied  { 0x2 } for msgtype=signal interface=org.freedesktop.login1.Manager member=SessionNew dest=org.freedesktop.DBus spid=691 tpid=720 scontext=system_u:system_r:systemd_logind_t:s0 tcontext=system_u:system_r:modemmanager_t:s0 tclass=(null)  exe="/usr/bin/dbus-daemon" sauid=81 hostname=? addr=? terminal=?'
				// type=USER_AVC msg=audit(1470087122.226:24068): pid=693 uid=81 auid=4294967295 ses=4294967295 subj=system_u:system_r:system_dbusd_t:s0-s0:c0.c1023 msg='avc:  denied  { 0x2 } for msgtype=signal interface=org.freedesktop.login1.Manager member=SessionRemoved dest=org.freedesktop.DBus spid=691 tpid=720 scontext=system_u:system_r:systemd_logind_t:s0 tcontext=system_u:system_r:modemmanager_t:s0 tclass=(null)  exe="/usr/bin/dbus-daemon" sauid=81 hostname=? addr=? terminal=?'
				avcRegex = "type=USER_AVC .* msgtype=signal interface=org.freedesktop.login1.Manager member=Session(New|Removed) dest=org.freedesktop.DBus .* exe=\"/usr/bin/dbus-daemon\" .*";
				if (!tailFromMarkedFile.isEmpty() && doesStringContainMatches(tailFromMarkedFile, avcRegex)) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1362273"; // Bug 1362273 - avc denied /var/log/audit/audit.log when "systemd: Started Session # of user root." is written to /var/log/messages every two minutes
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
					if (invokeWorkaroundWhileBugIsOpen) {
						log.warning("Ignoring the presence of AVC denials matching '"+avcRegex+"' while bug '"+bugId+"' is open.");
						tailFromMarkedFile = tailFromMarkedFile.replaceAll(avcRegex, "");
					}
				}
				// END OF WORKAROUND
				
				Assert.assertTrue(tailFromMarkedFile.trim().isEmpty(), "No SELinux denials found in the audit log '"+clienttasks.auditLogFile+"' on client "+clienttasks.sshCommandRunner.getConnection().getRemoteHostname()+" while executing this test class.");
			}
		}
	}
	
	public static String allRegisteredConsumerCertsDir = "/tmp/sm-allRegisteredConsumerCerts";
	@BeforeSuite(groups={"setup"},dependsOnMethods={"setupBeforeSuite"}, description="Prepare a temporary consumer cert directory where we can track all of the consumers created so we can return their entitlements after the suite.")
	public void prepareRegisteredConsumerCertsDirectoryBeforeSuite() {
		for (SubscriptionManagerTasks clienttasks : Arrays.asList(client1tasks,client2tasks)) {
			if (clienttasks!=null) {
///*debugTestSSL*/ if (false)
				clienttasks.sshCommandRunner.runCommandAndWait("rm -rf "+allRegisteredConsumerCertsDir);
				clienttasks.sshCommandRunner.runCommandAndWait("mkdir -p "+allRegisteredConsumerCertsDir);
			}
		}
	}
	@BeforeSuite(groups={"setup"},dependsOnMethods={"setupBeforeSuite"}, description="Dump the system's hardware info to a tar file and upload it for archival review.")
	public void dumpHardwareInfoBeforeSuite() throws IOException {
		if (client!=null) {
			// dump hardware info from dmidecode...
			if (clienttasks.isPackageInstalled("dmidecode")) {
				File remoteFile = new File("/root/hw_info_dump.tar");
				// [root@jsefler-5 ~]# dmidecode --dump-bin dmi_dump.bin; TMPDIR=`mktemp -d` && mkdir $TMPDIR/proc; cp /proc/cpuinfo $TMPDIR/proc/; cp /proc/sysinfo $TMPDIR/proc/; mkdir -p $TMPDIR/sys/devices/system/cpu; cp -r /sys/devices/system/cpu $TMPDIR/sys/devices/system/; tar -cf hw_info_dump.tar --ignore-failed-read --sparse dmi_dump.bin $TMPDIR  && tar -tvf hw_info_dump.tar && rm -rf $TMPDIR
				client.runCommandAndWait("dmidecode --dump-bin dmi_dump.bin; TMPDIR=`mktemp -d`; mkdir $TMPDIR/proc; cp /proc/cpuinfo $TMPDIR/proc/; cp /proc/sysinfo $TMPDIR/proc/; mkdir -p $TMPDIR/sys/devices/system/cpu; cp -r /sys/devices/system/cpu $TMPDIR/sys/devices/system/; tar -cf "+remoteFile.getName()+" --ignore-failed-read --sparse dmi_dump.bin $TMPDIR  && tar -tvf "+remoteFile.getName()+" && rm -rf $TMPDIR");
				if (!RemoteFileTasks.testExists(client, remoteFile.getPath())) client.runCommandAndWait("touch "+remoteFile.getPath());
				File localFile = new File((getProperty("automation.dir", "/tmp")+"/test-output/"+remoteFile.getName()));
				RemoteFileTasks.getFile(client, localFile.getParent(),remoteFile.getPath());
			}
			
			// dump hardware info from sosreport...
			if (clienttasks.isPackageInstalled("sos")) {
				String plugins = "hardware";
				if (Integer.valueOf(clienttasks.redhatReleaseX)>=5) plugins = "hardware";
				if (Integer.valueOf(clienttasks.redhatReleaseX)>=6) plugins = "hardware,powerpc";
				if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) plugins = "hardware,powerpc,processor,ceph";
				//	[root@jsefler-os7 ~]# sosreport --batch --only-plugins=processor,powerpc  --tmp-dir=/var/tmp
				//
				//	sosreport (version 3.2)
				//
				//	This command will collect diagnostic and configuration information from
				//	this Red Hat Enterprise Linux system and installed applications.
				//
				//	An archive containing the collected information will be generated in
				//	/var/tmp and may be provided to a Red Hat support representative.
				//
				//	Any information provided to Red Hat will be treated in accordance with
				//	the published support policies at:
				//
				//	  https://access.redhat.com/support/
				//
				//	The generated archive may contain data considered sensitive and its
				//	content should be reviewed by the originating organization before being
				//	passed to any third party.
				//
				//	No changes will be made to system configuration.
				//
				//
				//	 Setting up archive ...
				//	 Setting up plugins ...
				//	 Running plugins. Please wait ...
				//
				//	  Running 1/2: powerpc...        
				//	  Running 2/2: processor...        
				//
				//	Creating compressed archive...
				//
				//	Your sosreport has been generated and saved in:
				//	  /var/tmp/sosreport-jsefler-os7.usersys.redhat.com-20141013121912.tar.xz
				//
				//	The checksum is: 262a4a74df6c84e9d3dcd556dcd4abf0
				//
				//	Please send this file to your support representative.
				//
				//	[root@jsefler-os7 ~]# ls -l /var/tmp/sosreport-jsefler-os7.usersys.redhat.com-20141013121912.tar.xz
				//	-rw-r--r--. 1 root root 5444 Oct 13 12:19 /var/tmp/sosreport-jsefler-os7.usersys.redhat.com-20141013121912.tar.xz
				//	[root@jsefler-os7 ~]# 
				SSHCommandResult sosResult = client.runCommandAndWait("sosreport --batch --tmp-dir=/var/tmp --only-plugins="+plugins);
				File remoteFile = new File(getSubstringMatches(sosResult.getStdout(), "/var/tmp/sosreport-.+\\.tar\\.(xz|bz2)").get(0));	// /var/tmp/sosreport-jsefler-os7.usersys.redhat.com-20141013121912.tar.xz	// /var/tmp/sosreport-jsefler-5.usersys.redhat.com.tar.bz2
				if (RemoteFileTasks.testExists(client, remoteFile.getPath())) {
					File localFile = new File((getProperty("automation.dir", "/tmp")+"/test-output/"+remoteFile.getName()));
					RemoteFileTasks.getFile(client, localFile.getParent(),remoteFile.getPath());
					localFile.renameTo(new File(localFile.getPath().replaceFirst("-\\d{14}","")));	// strip out -20141013121912
				}
			}
		}
	}
	@AfterSuite(groups={"cleanup"},description="attempt to delete any abandoned entitlements granted during the run of this suite")
	public void deleteAllRegisteredConsumerEntitlementsAfterSuite() {
///*debugTestSSL*/ if(true) return;
		for (SubscriptionManagerTasks clienttasks : Arrays.asList(client1tasks,client2tasks)) {
			if (clienttasks!=null) {
				// determine the url to the server
				String url = "https://"+clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname")+":"+clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port")+clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
				
				//for (String consumerCertPath : Arrays.asList(clienttasks.sshCommandRunner.runCommandAndWait("find "+allRegisteredConsumerCertsDir+" -name '*cert.pem'").getStdout().trim().split("\n"))) {
				for (String consumerCertPath : Arrays.asList(clienttasks.sshCommandRunner.runCommandAndWait("ls -t1 "+allRegisteredConsumerCertsDir+"/*cert.pem").getStdout().trim().split("\n"))) {	// process consumers in reverse order of their creation
					if (consumerCertPath.isEmpty()) continue;
					// extract the uuid from the consumerCertPath
					String uuid = consumerCertPath.replace(allRegisteredConsumerCertsDir, "").replace("_cert.pem", "").replace("/","");
					// extract the key from the consumerCertPath
					String consumerKeyPath = consumerCertPath.replace("cert.pem", "key.pem");
					// curl -k --cert /etc/pki/consumer/cert.pem --key /etc/pki/consumer/key.pem -X DELETE https://subscription.rhn.stage.redhat.com/subscription/consumers/2f801f45-3b79-42ee-9013-f4ad5bd35c3a/entitlements
					clienttasks.sshCommandRunner.runCommandAndWait("curl --stderr /dev/null --insecure --cert "+consumerCertPath+" --key "+consumerKeyPath+" --request DELETE "+url+"/consumers/"+uuid+"/entitlements");
				}
			}
		}
	}
	
	@AfterSuite(groups={"cleanup"},description="subscription manager tear down")
	public void unregisterClientsAfterSuite() {
		for (SubscriptionManagerTasks clienttasks : Arrays.asList(client1tasks,client2tasks)) {
			if (clienttasks!=null) {
				clienttasks.unregister_(null, null, null, null);	// release the entitlements consumed by the current registration
				clienttasks.clean_();	// in case the unregister fails, also clean the client
			}
		}
	}
	
	@AfterSuite(groups={"cleanup"},description="subscription manager tear down")
	public void disconnectDatabaseAfterSuite() {
		
		// close the candlepin database connection
		if (servertasks.dbConnection!=null) {
			try {
				servertasks.dbConnection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	// close the connection to the database
		}
		
		// log candlepin's ending disk usage (for debugging information only)
		if (server!=null) {
			server.runCommandAndWait("df -h");
			server.runCommandAndWait("ls -Slh /var/log/tomcat6 | head");
		}
	}
	
	@AfterSuite(groups={"return2beaker"/*"cleanup"*/},description="return clients to beaker",dependsOnMethods={"disconnectDatabaseAfterSuite","unregisterClientsAfterSuite"}/*, alwaysRun=true*/)
	public void return2beaker() {
		for (SSHCommandRunner client : Arrays.asList(client1,client2)) {
			if (client!=null) {
				if (Boolean.valueOf(getProperty("sm.client.return2beaker","true"))) {
					client.runCommandAndWait("return2beaker.sh");	// return this client back to beaker
				}
			}
		}
	}
	
	
	@Deprecated
	@BeforeSuite(groups={"setup"},dependsOnMethods={"setupBeforeSuite"}, description="delete selected secondary/duplicate subscriptions to reduce the number of available pools against a standalone candlepin server")
	public void deleteSomeSecondarySubscriptionsBeforeSuite() throws JSONException, Exception {
		
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.2.0-1")) {	// effectively a permanent and suggested WORKAROUND prompted by Bug 1503578 - Runtime Error You can't operate on a closed ResultSet!!!
			// forward to newer task
			deleteSomeSecondarySubscriptionPoolsBeforeSuite();
			return;
		}
		
		Set<String> secondarySkusSkipped = new HashSet<String>();
		Set<String> secondarySkusToDelete = new HashSet<String>(Arrays.asList(new String[]{
		//	[jsefler@jseflerT5400 ~]$ curl --stderr /dev/null --insecure --user admin:admin --request GET https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/owners/admin/subscriptions?include=product.id | python -m simplejson/tool | grep \"id\" | sort | awk '{print $2}' | xargs -i[] echo \"[]\",
			"2cores-2ram-multiattr",
			"2cores-2ram-multiattr",
			"awesomeos-all-just-86_64-cont",
			"awesomeos-all-just-86_64-cont",
			"awesomeos-all-no-86_64-cont",
			"awesomeos-all-no-86_64-cont",
			"awesomeos-all-x86-cont",
			"awesomeos-all-x86-cont",
			"awesomeos-docker",
			"awesomeos-docker",
			"awesomeos-everything",
			"awesomeos-everything",
			"awesomeos-guestlimit-4-stackable",
			"awesomeos-guestlimit-4-stackable",
			"awesomeos-i386",
			"awesomeos-i386",
			"awesomeos-i686",
			"awesomeos-i686",
			"awesomeos-ia64",
			"awesomeos-ia64",
		//	"awesomeos-instancebased",		// kept because it will help test bug 963227
		//	"awesomeos-instancebased",
		//	"awesomeos-instancebased",	// future?
			"awesomeos-modifier",
			"awesomeos-modifier",
			"awesomeos-onesocketib",
			"awesomeos-onesocketib",
			"awesomeos-ostree",
			"awesomeos-ostree",
			"awesomeos-per-arch-cont",
			"awesomeos-per-arch-cont",
			"awesomeos-ppc64",
			"awesomeos-ppc64",
			"awesomeos-s390",
			"awesomeos-s390",
			"awesomeos-s390x",
			"awesomeos-s390x",
			"awesomeos-server",
			"awesomeos-server",
			"awesomeos-server-2-socket-std",
			"awesomeos-server-2-socket-std",
		//	"awesomeos-server-basic",		// kept because it is Multi-Entitlement: No
		//	"awesomeos-server-basic",
			"awesomeos-server-basic-dc",
			"awesomeos-server-basic-dc",
		//	"awesomeos-server-basic-me",	// kept because it is Multi-Entitlement: Yes
		//	"awesomeos-server-basic-me",
		//	"awesomeos-server-basic-me",	// future?
			"awesomeos-super-hypervisor",
			"awesomeos-super-hypervisor",
			"awesomeos-virt-4",
			"awesomeos-virt-4",
			"awesomeos-virt-datacenter",
			"awesomeos-virt-datacenter",
			"awesomeos-virt-unlimited",
			"awesomeos-virt-unlimited",
			"awesomeos-virt-unlmtd-phys",
			"awesomeos-virt-unlmtd-phys",
			"awesomeos-workstation-basic",
			"awesomeos-workstation-basic",
			"awesomeos-x86",
			"awesomeos-x86",
			"awesomeos-x86_64",
			"awesomeos-x86_64",
			"cores-26",
			"cores-26",
			"cores4-multiattr",
			"cores4-multiattr",
			"cores-8-stackable",
			"cores-8-stackable",
			"management-100",
			"management-100",
		//	"MKT-multiplier-client-50",
		//	"MKT-multiplier-client-50",
		//	"MKT-multiplier-client-50",	// future?
			"non-stacked-6core8ram-multiattr",
			"non-stacked-6core8ram-multiattr",
			"non-stacked-8core4ram-multiattr",
			"non-stacked-8core4ram-multiattr",
			"non-stacked-multiattr",
			"non-stacked-multiattr",
			"ram-2gb-stackable",
			"ram-2gb-stackable",
			"ram2-multiattr",
			"ram2-multiattr",
			"ram-4gb-stackable",
			"ram-4gb-stackable",
			"ram-8gb",
			"ram-8gb",
			"ram-cores-8gb-4cores",
			"ram-cores-8gb-4cores",
			"sfs",
			"sfs",
			"sock2-multiattr",
			"sock2-multiattr",
			"sock-core-ram-multiattr",
			"sock-core-ram-multiattr",
			"stackable-with-awesomeos-x86_64",
			"stackable-with-awesomeos-x86_64",
			"virt-awesomeos-i386",
			"virt-awesomeos-i386",
			""}));
		
		if (sm_clientOrg==null) return;
		if (!CandlepinType.standalone.equals(sm_serverType)) return;
		
		// process all of the subscriptions belonging to ownerKey
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+sm_clientOrg+"/subscriptions"));	// /subscriptions?include=id&include=product.id
		List<String> secondarySubscriptionIdsDeleted = new ArrayList<String>();
		for (int i = 0; i < jsonSubscriptions.length(); i++) {
			JSONObject jsonSubscription = (JSONObject) jsonSubscriptions.get(i);
			JSONObject jsonProduct = (JSONObject) jsonSubscription.get("product");
			String productId = jsonProduct.getString("id");
			
			// skip all skus not on the list of secondarySkusToDelete
			if (!secondarySkusToDelete.contains(productId)) continue;
			
			// TODO check the startDate and keep future subscriptions
			
			// skip the first secondarySkusToDelete encountered (this will be the one kept)
			if (!secondarySkusSkipped.contains(productId)) {
				secondarySkusSkipped.add(productId);
				continue;
			}
			
			// delete the subscription
			String subscriptionId = jsonSubscription.getString("id");
			CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl, "/subscriptions/"+subscriptionId);
			secondarySubscriptionIdsDeleted.add(subscriptionId);
		}
		
		// refresh the pools (only when some secondary subscriptions have been deleted) 
		if (!secondarySubscriptionIdsDeleted.isEmpty()) {
			JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,sm_clientOrg);
			jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,jobDetail,"FINISHED", 5*1000, 1);
		}
	}
	/**
	 * This is a replacement for deleteSomeSecondarySubscriptionsBeforeSuite() and a candlepin-2.2+ workaround for Bug 1503578 - Runtime Error You can't operate on a closed ResultSet!!!
	 * @throws JSONException
	 * @throws Exception
	 */
	public void deleteSomeSecondarySubscriptionPoolsBeforeSuite() throws JSONException, Exception {
		
		Set<String> secondarySkusSkipped = new HashSet<String>();
		Set<String> secondarySkusToDelete = new HashSet<String>(Arrays.asList(new String[]{
		//	[root@jsefler-rhel7 ~]# curl --stderr /dev/null --insecure --user admin:admin --request GET 'https://jsefler-candlepin.usersys.redhat.com:8443/candlepin/owners/admin/pools?add_future=true&include=productId' | python -m json/tool | grep \"productId\" | sort | awk '{print $2}' | xargs -i[] echo \"[]\",	
				"2cores-2ram-multiattr",
				"2cores-2ram-multiattr",
				"2cores-2ram-multiattr",
				"adminos-onesocketib",
				"adminos-onesocketib",
				"adminos-onesocketib",
				"adminos-onesocketib",
				"adminos-onesocketib",
				"adminos-onesocketib",
				"adminos-server-2-socket-std",
				"adminos-server-2-socket-std",
				"adminos-server-2-socket-std",
				"awesomeos-all-just-86_64-cont",
				"awesomeos-all-just-86_64-cont",
				"awesomeos-all-just-86_64-cont",
				"awesomeos-all-no-86_64-cont",
				"awesomeos-all-no-86_64-cont",
				"awesomeos-all-no-86_64-cont",
				"awesomeos-all-x86-cont",
				"awesomeos-all-x86-cont",
				"awesomeos-all-x86-cont",
				"awesomeos-docker",
				"awesomeos-docker",
				"awesomeos-docker",
				"awesomeos-everything",
				"awesomeos-everything",
				"awesomeos-everything",
				"awesomeos-guestlimit-4-stackable",
				"awesomeos-guestlimit-4-stackable",
				"awesomeos-guestlimit-4-stackable",
				"awesomeos-guestlimit-4-stackable",
				"awesomeos-guestlimit-4-stackable",
				"awesomeos-guestlimit-4-stackable",
				"awesomeos-i386",
				"awesomeos-i386",
				"awesomeos-i386",
				"awesomeos-i686",
				"awesomeos-i686",
				"awesomeos-i686",
				"awesomeos-ia64",
				"awesomeos-ia64",
				"awesomeos-ia64",
//				"awesomeos-instancebased",		// kept because it will help test bug 963227
//				"awesomeos-instancebased",
//				"awesomeos-instancebased",
//				"awesomeos-instancebased",
//				"awesomeos-instancebased",
//				"awesomeos-instancebased",
				"awesomeos-modifier",
				"awesomeos-modifier",
				"awesomeos-modifier",
				"awesomeos-onesocketib",
				"awesomeos-onesocketib",
				"awesomeos-onesocketib",
				"awesomeos-onesocketib",
				"awesomeos-onesocketib",
				"awesomeos-onesocketib",
				"awesomeos-ostree",
				"awesomeos-ostree",
				"awesomeos-ostree",
				"awesomeos-per-arch-cont",
				"awesomeos-per-arch-cont",
				"awesomeos-per-arch-cont",
				"awesomeos-ppc64",
				"awesomeos-ppc64",
				"awesomeos-ppc64",
				"awesomeos-s390",
				"awesomeos-s390",
				"awesomeos-s390",
				"awesomeos-s390x",
				"awesomeos-s390x",
				"awesomeos-s390x",
				"awesomeos-server",
				"awesomeos-server",
				"awesomeos-server",
				"awesomeos-server-2-socket-std",
				"awesomeos-server-2-socket-std",
				"awesomeos-server-2-socket-std",
//				"awesomeos-server-basic",		// kept because it is Multi-Entitlement: No
//				"awesomeos-server-basic",
//				"awesomeos-server-basic",
				"awesomeos-server-basic-dc",
				"awesomeos-server-basic-dc",
				"awesomeos-server-basic-dc",
//				"awesomeos-server-basic-me",	// kept because it is Multi-Entitlement: Yes
//				"awesomeos-server-basic-me",
//				"awesomeos-server-basic-me",
				"awesomeos-server-basic-vdc",
				"awesomeos-server-basic-vdc",
				"awesomeos-server-basic-vdc",
				"awesomeos-super-hypervisor",
				"awesomeos-super-hypervisor",
				"awesomeos-super-hypervisor",
				"awesomeos-super-hypervisor",
				"awesomeos-super-hypervisor",
				"awesomeos-super-hypervisor",
				"awesomeos-ul-quantity-virt",
				"awesomeos-ul-quantity-virt",
				"awesomeos-ul-quantity-virt",
				"awesomeos-ul-quantity-virt",
				"awesomeos-ul-quantity-virt",
				"awesomeos-ul-quantity-virt",
				"awesomeos-unlimited-quantity",
				"awesomeos-unlimited-quantity",
				"awesomeos-unlimited-quantity",
				"awesomeos-virt-4",
				"awesomeos-virt-4",
				"awesomeos-virt-4",
				"awesomeos-virt-4",
				"awesomeos-virt-4",
				"awesomeos-virt-4",
				"awesomeos-virt-datacenter",
				"awesomeos-virt-datacenter",
				"awesomeos-virt-datacenter",
				"awesomeos-virt-datacenter",
				"awesomeos-virt-datacenter",
				"awesomeos-virt-datacenter",
				"awesomeos-virt-unlimited",
				"awesomeos-virt-unlimited",
				"awesomeos-virt-unlimited",
				"awesomeos-virt-unlimited",
				"awesomeos-virt-unlimited",
				"awesomeos-virt-unlimited",
				"awesomeos-virt-unlmtd-phys",
				"awesomeos-virt-unlmtd-phys",
				"awesomeos-virt-unlmtd-phys",
				"awesomeos-virt-unlmtd-phys",
				"awesomeos-virt-unlmtd-phys",
				"awesomeos-virt-unlmtd-phys",
				"awesomeos-workstation-basic",
				"awesomeos-workstation-basic",
				"awesomeos-workstation-basic",
				"awesomeos-x86",
				"awesomeos-x86",
				"awesomeos-x86",
				"awesomeos-x86_64",
				"awesomeos-x86_64",
				"awesomeos-x86_64",
				"cores-26",
				"cores-26",
				"cores-26",
				"cores4-multiattr",
				"cores4-multiattr",
				"cores4-multiattr",
				"cores-8-stackable",
				"cores-8-stackable",
				"cores-8-stackable",
				"management-100",
				"management-100",
				"management-100",
//				"MKT-multiplier-client-50",
//				"MKT-multiplier-client-50",
//				"MKT-multiplier-client-50",
				"non-stacked-6core8ram-multiattr",
				"non-stacked-6core8ram-multiattr",
				"non-stacked-6core8ram-multiattr",
				"non-stacked-8core4ram-multiattr",
				"non-stacked-8core4ram-multiattr",
				"non-stacked-8core4ram-multiattr",
				"non-stacked-multiattr",
				"non-stacked-multiattr",
				"non-stacked-multiattr",
				"ram-2gb-stackable",
				"ram-2gb-stackable",
				"ram-2gb-stackable",
				"ram2-multiattr",
				"ram2-multiattr",
				"ram2-multiattr",
				"ram-4gb-stackable",
				"ram-4gb-stackable",
				"ram-4gb-stackable",
				"ram-8gb",
				"ram-8gb",
				"ram-8gb",
				"ram-cores-8gb-4cores",
				"ram-cores-8gb-4cores",
				"ram-cores-8gb-4cores",
				"sfs",
				"sfs",
				"sfs",
				"sock2-multiattr",
				"sock2-multiattr",
				"sock2-multiattr",
				"sock-core-ram-multiattr",
				"sock-core-ram-multiattr",
				"sock-core-ram-multiattr",
				"stackable-with-awesomeos-x86_64",
				"stackable-with-awesomeos-x86_64",
				"stackable-with-awesomeos-x86_64",
//				"storage-limited-256",	// keep multiple pools to testAutoSubscribeStorageBandSubscription() and testAutoHealStorageBandSubscription()
//				"storage-limited-256",
//				"storage-limited-256",
				"virt-awesomeos-i386",
				"virt-awesomeos-i386",
				"virt-awesomeos-i386",
			""}));
		
		if (sm_clientOrg==null) return;
		if (!CandlepinType.standalone.equals(sm_serverType)) return;
		
		// process all of the subscription pools belonging to ownerKey
		JSONArray jsonSubscriptionPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+sm_clientOrg+"/pools?add_future=true"));	// /pools?add_future=true&include=productId
		for (int i = 0; i < jsonSubscriptionPools.length(); i++) {
			JSONObject jsonSubscriptionPool = (JSONObject) jsonSubscriptionPools.get(i);
			String productId = jsonSubscriptionPool.getString("productId");
			
			// skip all subscription pools for productIds (a.k.a. SKUs) not on the list of secondarySkusToDelete
			if (!secondarySkusToDelete.contains(productId)) continue;
			
			// skip all custom pools not originating from a subscription
			if (jsonSubscriptionPool.isNull("subscriptionSubKey")) continue;
			
			String subscriptionSubKey = jsonSubscriptionPool.getString("subscriptionSubKey");	// master or derived
			
			// skip non-master subscription pools
			if (!subscriptionSubKey.equals("master")) continue;
			
			// TODO check the startDate and keep future subscription pools
			
			// skip the first secondarySkusToDelete encountered (this will be the one kept)
			if (!secondarySkusSkipped.contains(productId)) {
				secondarySkusSkipped.add(productId);
				continue;
			}
			
			// delete the subscription pool
			String id = jsonSubscriptionPool.getString("id");
			CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl, "/pools/"+id);
		}
	}
	
	
	
	protected static ArrayList<String> invokedWorkaroundBugs = new ArrayList<String>();;
	@AfterSuite(groups={"cleanup"},description="log all the invoked bugzilla workarounds")
	public void logInvokedWorkaroundsAfterSuite() {
		Collections.sort(invokedWorkaroundBugs);
		if (!invokedWorkaroundBugs.isEmpty()) log.info(String.format("There were %s workarounds invoked for bugs in this run: https://bugzilla.redhat.com/buglist.cgi?bug_id=%s",invokedWorkaroundBugs.size(),joinListToString(invokedWorkaroundBugs,",")));
	}
	public static void addInvokedWorkaround(String bugId) {
		if (!invokedWorkaroundBugs.contains(bugId)) invokedWorkaroundBugs.add(bugId);
	}
	
	// Protected Methods ***********************************************************************


	
	protected String getServerUrl(String hostname, String port, String prefix) {
		// https://hostname:port/prefix
		if (!port.equals("")) port=(":"+port).replaceFirst("^:+", ":");
		if (!prefix.equals("")) prefix=("/"+prefix).replaceFirst("^/+", "/");
		return "https://"+hostname+port+prefix;	
	}
	

	/* DELETEME  OLD CODE FROM ssalevan
	
	public void getSalesToEngineeringProductBindings(){
		try {
			String products = itDBConnection.nativeSQL("select * from butt;");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			log.info("Database query for Sales-to-Engineering product bindings failed!  Traceback:\n"+e.getMessage());
		}
	}
	*/
	

	public static void sleep(long milliseconds) {
		log.info("Sleeping for "+milliseconds+" milliseconds...");
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			log.info("Sleep interrupted!");
		}
	}
	
	public static int getRandInt(){
		return Math.abs(randomGenerator.nextInt());
	}
	
	/**
	 * @param list - the list from which to return a subset of its contents
	 * @param subsetSize - specify the subset size desired 0<=subsetSize<=list.size();  if the value specified is outside this range, it will be adjusted.
	 * @return a random subset of the list
	 */
	public static <T> List<T> getRandomSubsetOfList(List<T> list,int subsetSize) {
		if (subsetSize > list.size()) subsetSize = list.size();	// limit subsetSize to the size of the list
		if (subsetSize < 0) subsetSize = 0;
		List<T> clonedList = new ArrayList<T>(list);
		List<T> subsetList = new ArrayList<T>(subsetSize);
		for (int i = 0; i < subsetSize; i++) subsetList.add(clonedList.remove(randomGenerator.nextInt(clonedList.size())));
		return subsetList;
	}
	
	/**
	 * @param list
	 * @return the same list in random order
	 */
	public static <T> List<T> getRandomList(List<T> list) {
		return getRandomSubsetOfList(list,list.size());
	}
	
	/**
	 * @param list
	 * @return 
	 * @return a random item from the list (return null if the list is empty)
	 */
	public static <T> T getRandomListItem(List<T> list) {
		List<T> sublist = getRandomSubsetOfList(list,1);
		if (sublist.isEmpty()) return null; 
		return sublist.get(0);
	}
	
	/**
	 * @return a random Boolean.TRUE or Boolean.FALSE
	 */
	public static Boolean getRandomBoolean() {
		return 	getRandomListItem(Arrays.asList(new Boolean[]{Boolean.TRUE,Boolean.FALSE}));
	}
	
	/**
	 * Randomize the input string case.  For example input="Hello World", output="HElLo wORlD"
	 * @param string
	 * @return
	 */
	protected String randomizeCaseOfCharactersInString(String string) {
		String output = "";
		for (int i=0; i<string.length(); i++) {
			String c = string.substring(i, i+1);
			output+=randomGenerator.nextInt(2)==0? c.toUpperCase():c.toLowerCase();
		}
		return output;
	}
	
	public static boolean isStringSimpleASCII(String str) {
		// Reference: http://www.asciitable.com/
	    for (int i = 0, n = str.length(); i < n; i++) {
	        if (str.charAt(i) > 127) { return false; }
	    }
	    return true;
	}
	
	public static boolean isInteger(String string) {  
	   try {  
	      Integer.parseInt(string);  
	      return true;  
	   }  
	   catch(NumberFormatException e) {  
	      return false;  
	   }  
	}
	
	public static boolean isFloat(String string) {  
		   try {  
		      Float.parseFloat(string);  
		      return true;  
		   }  
		   catch(NumberFormatException e) {  
		      return false;  
		   }  
		} 

	/**
	 * @param <T>
	 * @param list1
	 * @param list2
	 * @return true when lists have the same contents and same size, but not necessarily the same content order
	 */
	public static <T> boolean isEqualNoOrder(List<T> list1, List<T> list2) {  
	   return list1.containsAll(list2) && list2.containsAll(list1) && list1.size()==list2.size();
	   // similar to Assert.assertEqualsNoOrder(Object[] actual, Object[] expected, String message)
	}
	
	/**
	 * Given two Strings representing a list of comma separated tags, return true if they are effectively equivalent.<br>
	 * Example: "tag1,tag2" is equivalent to "tag2,tag1"<br>
	 * Example: "tag1,tag2" is NOT equivalent to "tag1,tag2,tag3"<br>
	 * @param requiredTagsAsString
	 * @param providedTagsAsString
	 * @return
	 */
	public boolean areTagsEquivalent(String requiredTagsAsString, String providedTagsAsString) {
		if (requiredTagsAsString==null && providedTagsAsString==null) return true;
		if (requiredTagsAsString==null && providedTagsAsString!=null &&  providedTagsAsString.isEmpty()) return true;
		if (requiredTagsAsString==null && providedTagsAsString!=null && !providedTagsAsString.isEmpty()) return false;
		if (providedTagsAsString==null && requiredTagsAsString!=null &&  requiredTagsAsString.isEmpty()) return true;
		if (providedTagsAsString==null && requiredTagsAsString!=null && !requiredTagsAsString.isEmpty()) return false;
		
		List<String> requiredTags = Arrays.asList(requiredTagsAsString.split("\\s*,\\s*"));
		List<String> providedTags = Arrays.asList(providedTagsAsString.split("\\s*,\\s*"));
		
		return (requiredTags.containsAll(providedTags) && providedTags.containsAll(requiredTags));
	}
	
	/**
	 * @param <T>
	 * @param list1
	 * @param list2
	 * @return true if list1.containsAny(list2)
	 */
	public static <T> boolean doesListOverlapList(List<T> list1, List<T> list2) {
		for (T list2item : list2) if (list1.contains(list2item)) return true;
		return false;
	}
	
	static public String joinListToString(List<String> list, String conjunction) {
	   StringBuilder sb = new StringBuilder();
	   boolean first = true;
	   for (String item : list) {
	      if (first) first = false;
	      else sb.append(conjunction);
	      sb.append(item);
	   }
	   return sb.toString();
	}
	
	static public List<String> getSubstringMatches(String string, String subStringRegex) {
		Pattern pattern = Pattern.compile(subStringRegex, Pattern.MULTILINE/* | Pattern.DOTALL*/);
		Matcher matcher = pattern.matcher(string);
		List<String> substringMatches = new ArrayList<String>();
		while (matcher.find()) substringMatches.add(matcher.group());
		return substringMatches;
	}
	
	static public boolean doesStringContainMatches(String string, String subStringRegex) {
		return !getSubstringMatches(string, subStringRegex).isEmpty();
	}
	
	
	/**
	 * @param runner1
	 * @param runner2
	 * @return the millisecond time difference between the system clocks (runner2 - runner1) (Note: if positive then runner 2 is ahead of runner1)
	 * @throws ParseException
	 */
	static public long getTimeDifferenceBetweenCommandRunners(SSHCommandRunner runner1, SSHCommandRunner runner2) throws ParseException {
		SimpleDateFormat unixFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
		String runner1DateStr = runner1.runCommandAndWait("date \"+%a %b %d %T %Z %Y\"").getStdout();
		String runner2DateStr = runner2.runCommandAndWait("date \"+%a %b %d %T %Z %Y\"").getStdout();
		Date runner1Date = unixFormat.parse(runner1DateStr);
		Date runner2Date = unixFormat.parse(runner2DateStr);
		//long msTimeDiff = Math.abs(runner2Date.getTime() - runner1Date.getTime());
		long msTimeDiff = runner2Date.getTime() - runner1Date.getTime();
		return msTimeDiff;
	}
	
	// Protected Inner Data Class ***********************************************************************
	
	protected class RegistrationData {
		public String username=null;
		public String password=null;
		public String ownerKey=null;
		public SSHCommandResult registerResult=null;
		public List<SubscriptionPool> allAvailableSubscriptionPools=null;/*new ArrayList<SubscriptionPool>();*/
		public RegistrationData() {
			super();
		}
		public RegistrationData(String username, String password, String ownerKey,	SSHCommandResult registerResult, List<SubscriptionPool> allAvailableSubscriptionPools) {
			super();
			this.username = username;
			this.password = password;
			this.ownerKey = ownerKey;
			this.registerResult = registerResult;
			this.allAvailableSubscriptionPools = allAvailableSubscriptionPools;
		}
		
		public String toString() {
			String string = "";
			if (username != null)		string += String.format(" %s='%s'", "username",username);
			if (password != null)		string += String.format(" %s='%s'", "password",password);
			if (ownerKey != null)		string += String.format(" %s='%s'", "ownerKey",ownerKey);
			if (registerResult != null)	string += String.format(" %s=[%s]", "registerResult",registerResult);
			for (SubscriptionPool subscriptionPool : allAvailableSubscriptionPools) {
				string += String.format(" %s=[%s]", "availableSubscriptionPool",subscriptionPool);
			}
			return string.trim();
		}
	}
	
	// this list will be populated by subclass ResisterTests.testRegisterWithCredentials
	protected static List<RegistrationData> registrationDataList = new ArrayList<RegistrationData>();	

//	/**
//	 * Useful when trying to find a username that belongs to a different owner/org than the current username you are testing with.
//	 * @param key
//	 * @return null when no match is found
//	 * @throws JSONException
//	 */
//	protected RegistrationData findRegistrationDataNotMatchingOwnerKey(String key) throws JSONException {
//		Assert.assertTrue (!registrationDataList.isEmpty(), "The testRegisterWithCredentials has been executed thereby populating the registrationDataList with content for testing."); 
//		for (RegistrationData registration : registrationDataList) {
//			if (registration.ownerKey!=null) {
//				if (!registration.ownerKey.equals(key)) {
//					return registration;
//				}
//			}
//		}
//		return null;
//	}
	
	/**
	 * Useful when trying to find registerable credentials that belongs to a different (or same) owner than the current credentials you are testing with.
	 * @param matchingUsername
	 * @param username
	 * @param matchingOwnerKey
	 * @param ownerkey
	 * @return
	 * @throws JSONException
	 */
	protected List<RegistrationData> findGoodRegistrationData(Boolean matchingUsername, String username, Boolean matchingOwnerKey, String ownerKey) throws JSONException {
		List<RegistrationData> finalRegistrationData = new ArrayList<RegistrationData>();
		List<RegistrationData> goodRegistrationData = new ArrayList<RegistrationData>();
		List<String> ownersWithMatchingUsername = new ArrayList<String>();
		List<String> usernamesWithMatchingOwnerKey = new ArrayList<String>();
		Assert.assertTrue (!registrationDataList.isEmpty(), "The testRegisterWithCredentials has been executed thereby populating the registrationDataList with content for testing."); 
		for (RegistrationData registrationDatum : registrationDataList) {
			if (registrationDatum.registerResult.getExitCode().intValue()==0) {
				if (registrationDatum.ownerKey.equals(ownerKey)) usernamesWithMatchingOwnerKey.add(registrationDatum.username);
				if (registrationDatum.username.equals(username)) ownersWithMatchingUsername.add(registrationDatum.ownerKey);
				if ( matchingUsername &&  registrationDatum.username.equals(username) &&  matchingOwnerKey &&  registrationDatum.ownerKey.equals(ownerKey)) {
					goodRegistrationData.add(registrationDatum);
				}
				if ( matchingUsername &&  registrationDatum.username.equals(username) && !matchingOwnerKey && !registrationDatum.ownerKey.equals(ownerKey)) {
					goodRegistrationData.add(registrationDatum);
				}
				if (!matchingUsername && !registrationDatum.username.equals(username) &&  matchingOwnerKey &&  registrationDatum.ownerKey.equals(ownerKey)) {
					goodRegistrationData.add(registrationDatum);
				}
				if (!matchingUsername && !registrationDatum.username.equals(username) && !matchingOwnerKey && !registrationDatum.ownerKey.equals(ownerKey)) {
					goodRegistrationData.add(registrationDatum);
				}
			}
		}
		for (RegistrationData registrationDatum : goodRegistrationData) {
				if (ownerKey==null && !matchingOwnerKey &&  !matchingUsername) {
					if (!registrationDatum.username.equals(username)) {
						finalRegistrationData.add(registrationDatum);
					}
				} else if (ownerKey==null && !matchingOwnerKey && matchingUsername) {					
					if (registrationDatum.username.equals(username)) {
						finalRegistrationData.add(registrationDatum);
					}
				}
		}
		for (RegistrationData registrationDatum : goodRegistrationData) {
			if ( !matchingOwnerKey &&  !matchingUsername) {
				if (!ownersWithMatchingUsername.contains(registrationDatum.ownerKey )&& !usernamesWithMatchingOwnerKey.contains(registrationDatum.username)) {
					finalRegistrationData.add(registrationDatum);
				}
			} else if ( !matchingOwnerKey && matchingUsername) {					
				if (!ownersWithMatchingUsername.contains(registrationDatum.ownerKey) && username.equals(registrationDatum.username)) {
					finalRegistrationData.add(registrationDatum);
				}
			} else if ( matchingOwnerKey && !matchingUsername ) {
				if (ownersWithMatchingUsername.contains(registrationDatum.ownerKey) && !username.equals(registrationDatum.username)) {
					finalRegistrationData.add(registrationDatum);
				}
			} else {
				finalRegistrationData.add(registrationDatum);
			}
		}
		return finalRegistrationData;
	}
	
//	/**
//	 * Useful when trying to find a username that belongs to the same owner/org as the current username you are testing with.
//	 * @param key
//	 * @param username
//	 * @return null when no match is found
//	 * @throws JSONException
//	 */
//	protected RegistrationData findRegistrationDataMatchingOwnerKeyButNotMatchingUsername(String key, String username) throws JSONException {
//		Assert.assertTrue (!registrationDataList.isEmpty(), "The testRegisterWithCredentials has been executed thereby populating the registrationDataList with content for testing."); 
//		for (RegistrationData registration : registrationDataList) {
//			if (registration.ownerKey!=null) {
//				if (registration.ownerKey.equals(key)) {
//					if (!registration.username.equals(username)) {
//						return registration;
//					}
//				}
//			}
//		}
//		return null;
//	}
//	
//	/**
//	 * Useful when trying to find registration data results from a prior registration by a given username.
//	 * @param key
//	 * @param username
//	 * @return null when no match is found
//	 * @throws JSONException
//	 */
//	protected RegistrationData findRegistrationDataMatchingUsername(String username) throws JSONException {
//		Assert.assertTrue (!registrationDataList.isEmpty(), "The testRegisterWithCredentials has been executed thereby populating the registrationDataList with content for testing."); 
//		for (RegistrationData registration : registrationDataList) {
//			if (registration.username.equals(username)) {
//				return registration;
//			}
//		}
//		return null;
//	}
	
	/**
	 * This can be called by Tests that depend on it in a BeforeClass method to insure that registrationDataList has been populated.
	 * @throws Exception 
	 */
	protected void testRegisterWithCredentials() throws Exception {
		if (registrationDataList.isEmpty()) {
			clienttasks.unregister(null,null,null, null); // make sure client is unregistered
			for (List<Object> credentials : getRegisterCredentialsDataAsListOfLists()) {
				rhsm.cli.tests.RegisterTests registerTests = new rhsm.cli.tests.RegisterTests();
				registerTests.setupBeforeSuite();
				try {
					registerTests.testRegisterWithCredentials((String)credentials.get(0), (String)credentials.get(1), (String)credentials.get(2));			
				} catch (AssertionError e) {
					log.warning("Ignoring a failure in testRegisterWithCredentials("+(String)credentials.get(0)+", "+(String)credentials.get(1)+", "+(String)credentials.get(2)+")");
				}
			}
		}
	}
	
	
	/**
	 * @param flag 1=locked 0=unlocked
	 * @throws SQLException 
	 * @throws Exception
	 */
	protected void updateProductAndContentLockStateOnDatabase(int flag) throws SQLException   {
		
		// Candlepin introduced product and content locking to prevent undesirable modifications to
		// Red Hat resources via the RestAPI when candlepin runs in hosted mode (/etc/candlepin/candlepin.conf > "candlepin.standalone", "false")
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">", "2.0.0-1")) {	// candlepin commit FIXME
			
			// Avoid locked products and content after toggling candlepin.conf mode to "candlepin.standalone", "false"
			// 201702071658:23.304 - INFO: SSH alternative to HTTP request: curl --stderr /dev/null --insecure --user admin:admin --request DELETE https://jsefler-candlepin.usersys.redhat.com:8443/candlepin/owners/admin/products/99000 (rhsm.cli.tasks.CandlepinTasks.deleteResourceUsingRESTfulAPI)
			// 201702071658:23.383 - WARNING: Attempt to DELETE resource '/owners/admin/products/99000' failed: product "99000" is locked (rhsm.cli.tasks.CandlepinTasks.deleteResourceUsingRESTfulAPI)
			
			// SQL statement to update the database product and content table to unlocked
			String updateProductLockedStateSql = "UPDATE cp2_products SET locked = "+flag+";";
			String updateContentLockedStateSql = "UPDATE cp2_content SET locked = "+flag+";";
			
			Statement sql = servertasks.dbConnection.createStatement();
			int rowCount;
			
			log.fine("Executing SQL: "+updateProductLockedStateSql);
			rowCount = sql.executeUpdate(updateProductLockedStateSql);
			Assert.assertTrue(rowCount>0, "Updated at least one row (actual='"+rowCount+"') of the cp2_products table with sql: "+updateProductLockedStateSql);	// NOTE: the rowCount is not really the number of rows changed, its the number of rows retrieved by the where clause
			
			log.fine("Executing SQL: "+updateContentLockedStateSql);
			rowCount = sql.executeUpdate(updateContentLockedStateSql);
			Assert.assertTrue(rowCount>0, "Updated at least one row (actual='"+rowCount+"') of the cp2_content table with sql: "+updateContentLockedStateSql);	// NOTE: the rowCount is not really the number of rows changed, its the number of rows retrieved by the where clause
			
			sql.close();
		}
	}		
	
	
	/**
	 * On the connected candlepin server database, update the startdate and enddate in the cp_subscription table on rows where the pool id is a match.
	 * @param pool
	 * @param startDate
	 * @param endDate
	 * @throws Exception 
	 */
	protected void updateSubscriptionPoolDatesOnDatabase(SubscriptionPool pool, Calendar startDate, Calendar endDate) throws Exception {
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<=", "2.0.2-1")) {	// candlepin commit 957f5dcc2e47c88973f86317ed32033ad1c4f92f
			updateSubscriptionPoolDatesOnDatabase_PRE_CANDLEPIN_2(pool, startDate, endDate);
			return;
		}
		
		// get the master pool id (NEEDED BECAUSE WE WANT THE UPDATE THE DATA ON THE MASTER POOL, NOT THE SUB POOL)
		String masterPoolIdSqlQuery = String.format("SELECT pool_id FROM cp2_pool_source_sub WHERE subscription_sub_key = 'master' AND subscription_id = (SELECT subscription_id FROM cp2_pool_source_sub WHERE pool_id = '%s')",pool.poolId);
		Statement sql = servertasks.dbConnection.createStatement();
		ResultSet resultSet = sql.executeQuery(masterPoolIdSqlQuery);
		resultSet.next();	// assume only one row was returned
		String masterPoolId = resultSet.getString(1); // assumes only one column was returned
		sql.close();
		
		// get the json pool
		String poolResource = "/pools/"+pool.poolId;
			   poolResource = "/pools/"+masterPoolId;
		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, poolResource));
		// get the json Marketing Product
		JSONObject jsonMarketingProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, jsonPool.getJSONObject("owner").getString("href")+"/products/"+jsonPool.getString("productId")));
		String instanceMultiplier = CandlepinTasks.getPoolProductAttributeValue(jsonPool, "instance_multiplier");
		
		// get a slimmed down version of the pool
		String exclude = "?exclude=created"	// excluding data feels like the right thing to do, but could be the wrong thing if it is not calculated data - TODO: review with devel
				+ "&exclude=consumed"
				+ "&exclude=calculatedAttributes"
				+ "&exclude=productId"
				+ "&exclude=productName"
				+ "&exclude=productAttributes"
				+ "&exclude=providedProducts"
				+ "&exclude=derivedProductId"
				+ "&exclude=derivedProductName"
				+ "&exclude=derivedProvidedProducts"
				+ "&exclude=derivedProductAttributes";
		jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, poolResource+exclude));
		
		// re-adjust the quantity on the json pool when instance based
		if (instanceMultiplier!=null) {
			jsonPool.put("quantity", jsonPool.getInt("quantity")/Integer.valueOf(instanceMultiplier));
		}
		
		// re-adjust the quantity on the json pool when the marketing product has a multiplier
		if (jsonMarketingProduct.has("multiplier")) {
			jsonPool.put("quantity", jsonPool.getInt("quantity")/jsonMarketingProduct.getInt("multiplier"));
		}
		
		// change the start/end dates on the json pool
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		if (startDate!=null) jsonPool.put("startDate",formatter.format(startDate.getTime()));
		if (endDate!=null) jsonPool.put("endDate",formatter.format(endDate.getTime()));
		
		// PUT the updated json pool back
		String ownerHref = jsonPool.getJSONObject("owner").getString("href");
		poolResource = ownerHref+"/pools";
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, poolResource, jsonPool);

	}
	/**
	 * On the connected candlepin server database, update the startdate and enddate in the cp_subscription table on rows where the pool id is a match.
	 * @param pool
	 * @param startDate
	 * @param endDate
	 * @throws SQLException 
	 */
	protected void updateSubscriptionPoolDatesOnDatabase_PRE_CANDLEPIN_2(SubscriptionPool pool, Calendar startDate, Calendar endDate) throws SQLException {
		//DateFormat dateFormat = new SimpleDateFormat(CandlepinAbstraction.dateFormat);
		String updateSubscriptionPoolEndDateSql = "";
		String updateSubscriptionPoolStartDateSql = "";
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		if (endDate!=null) {
//			updateSubscriptionPoolEndDateSql = "update cp_subscription set enddate='"+EntitlementCert.formatDateString(endDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";	// valid prior to candlepin commit 86afa233b2fef2581f6eaa4e68a6eca1d4a657a0
//			updateSubscriptionPoolEndDateSql = "update cp_subscription set enddate='"+EntitlementCert.formatDateString(endDate)+"' where id=(select subscriptionid from cp_pool_source_sub where pool_id='"+pool.poolId+"');";
			
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">", "2.0.2-1")) {	// candlepin commit 957f5dcc2e47c88973f86317ed32033ad1c4f92f
//				updateSubscriptionPoolEndDateSql = String.format("UPDATE cp_pool SET enddate='%s' WHERE id = (SELECT pool_id FROM cp2_pool_source_sub WHERE subscription_sub_key = 'master' AND subscription_id = (SELECT subscription_id FROM cp2_pool_source_sub WHERE pool_id = '%s'))",formatter.format(endDate.getTime()),pool.poolId);
				updateSubscriptionPoolEndDateSql = String.format("UPDATE cp_pool SET enddate='%s' WHERE id = (SELECT pool_id FROM cp2_pool_source_sub WHERE subscription_sub_key = 'master' AND subscription_id = (SELECT subscription_id FROM cp2_pool_source_sub WHERE pool_id = '%s'))",EntitlementCert.formatDateString(endDate),pool.poolId);
			} else if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0-1")) {
				updateSubscriptionPoolEndDateSql = "update cp_pool set enddate='"+EntitlementCert.formatDateString(endDate)+"' where id='"+pool.poolId+"';";
			} else if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.9.11-1")) {	// candlepin commit 86afa233b2fef2581f6eaa4e68a6eca1d4a657a0
				updateSubscriptionPoolEndDateSql = "update cp_subscription set enddate='"+EntitlementCert.formatDateString(endDate)+"' where id=(select subscriptionid from cp_pool_source_sub where pool_id='"+pool.poolId+"');";
			} else {
				//updateSubscriptionPoolEndDateSql = "update cp_subscription set enddate='"+AbstractCommandLineData.formatDateString(endDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";
				// AbstractCommandLineData.formatDateString does not default to enough significant figures, changing to EntitlementCert.formatDateString...
				updateSubscriptionPoolEndDateSql = "update cp_subscription set enddate='"+EntitlementCert.formatDateString(endDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";
			}
		}
		if (startDate!=null) {
//			updateSubscriptionPoolStartDateSql = "update cp_subscription set startdate='"+EntitlementCert.formatDateString(startDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";	// valid prior to candlepin commit 86afa233b2fef2581f6eaa4e68a6eca1d4a657a0
//			updateSubscriptionPoolStartDateSql = "update cp_subscription set startdate='"+EntitlementCert.formatDateString(startDate)+"' where id=(select subscriptionid from cp_pool_source_sub where pool_id='"+pool.poolId+"');";
			
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">", "2.0.2-1")) {	// candlepin commit 957f5dcc2e47c88973f86317ed32033ad1c4f92f
//				updateSubscriptionPoolStartDateSql = String.format("UPDATE cp_pool SET startdate='%s' WHERE id = (SELECT pool_id FROM cp2_pool_source_sub WHERE subscription_sub_key = 'master' AND subscription_id = (SELECT subscription_id FROM cp2_pool_source_sub WHERE pool_id = '%s'))",formatter.format(startDate.getTime()),pool.poolId);
				updateSubscriptionPoolStartDateSql = String.format("UPDATE cp_pool SET startdate='%s' WHERE id = (SELECT pool_id FROM cp2_pool_source_sub WHERE subscription_sub_key = 'master' AND subscription_id = (SELECT subscription_id FROM cp2_pool_source_sub WHERE pool_id = '%s'))",EntitlementCert.formatDateString(startDate),pool.poolId);
			} else if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0-1")) {
				updateSubscriptionPoolStartDateSql = "update cp_pool set startdate='"+EntitlementCert.formatDateString(startDate)+"' where id='"+pool.poolId+"';";
			} else if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.9.11-1")) {	// candlepin commit 86afa233b2fef2581f6eaa4e68a6eca1d4a657a0
				updateSubscriptionPoolStartDateSql = "update cp_subscription set startdate='"+EntitlementCert.formatDateString(startDate)+"' where id=(select subscriptionid from cp_pool_source_sub where pool_id='"+pool.poolId+"');";
			} else {
				//updateSubscriptionPoolStartDateSql = "update cp_subscription set startdate='"+AbstractCommandLineData.formatDateString(startDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";
				// AbstractCommandLineData.formatDateString does not default to enough significant figures, changing to EntitlementCert.formatDateString...
				updateSubscriptionPoolStartDateSql = "update cp_subscription set startdate='"+EntitlementCert.formatDateString(startDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";	// valid prior to candlepin commit 86afa233b2fef2581f6eaa4e68a6eca1d4a657a0
			}

		}
		
		Statement sql = servertasks.dbConnection.createStatement();
		if (endDate!=null) {
			log.info("About to change the endDate in the database for this subscription pool: "+pool);
			log.fine("Executing SQL: "+updateSubscriptionPoolEndDateSql);
			Assert.assertEquals(sql.executeUpdate(updateSubscriptionPoolEndDateSql), 1, "Updated one row of the cp_subscription table with sql: "+updateSubscriptionPoolEndDateSql);
		}
		if (startDate!=null) {
			log.info("About to change the startDate in the database for this subscription pool: "+pool);
			log.fine("Executing SQL: "+updateSubscriptionPoolStartDateSql);
			Assert.assertEquals(sql.executeUpdate(updateSubscriptionPoolStartDateSql), 1, "Updated one row of the cp_subscription table with sql: "+updateSubscriptionPoolStartDateSql);
		}
		sql.close();
	}
	
	protected void updateSubscriptionDatesOnDatabase(String subscriptionId, Calendar startDate, Calendar endDate) throws SQLException {
		//DateFormat dateFormat = new SimpleDateFormat(CandlepinAbstraction.dateFormat);
		String updateSubscriptionEndDateSql = "";
		String updateSubscriptionStartDateSql = "";
		if (endDate!=null) {
			updateSubscriptionEndDateSql = "update cp_subscription set enddate='"+AbstractCommandLineData.formatDateString(endDate)+"' where id='"+subscriptionId+"';";
		}
		if (startDate!=null) {
			updateSubscriptionStartDateSql = "update cp_subscription set startdate='"+AbstractCommandLineData.formatDateString(startDate)+"' where id='"+subscriptionId+"';";
		}
		
		Statement sql = servertasks.dbConnection.createStatement();
		if (endDate!=null) {
			log.info("About to change the endDate in the database for this subscription id: "+subscriptionId);
			log.fine("Executing SQL: "+updateSubscriptionEndDateSql);
			Assert.assertEquals(sql.executeUpdate(updateSubscriptionEndDateSql), 1, "Updated one row of the cp_subscription table with sql: "+updateSubscriptionEndDateSql);
		}
		if (startDate!=null) {
			log.info("About to change the startDate in the database for this subscription id: "+subscriptionId);
			log.fine("Executing SQL: "+updateSubscriptionStartDateSql);
			Assert.assertEquals(sql.executeUpdate(updateSubscriptionStartDateSql), 1, "Updated one row of the cp_subscription table with sql: "+updateSubscriptionStartDateSql);
		}
		sql.close();
	}
	
	// 12/8/2015 candlepin dropped the milliseconds on the reported date pattern in candlepin commit 16b18540f84443a19786b2f97773e481ecd3c011 hence these are no longer final
	//final String iso8601DatePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; 								//"2012-02-08T00:00:00.000+0000"
	//final DateFormat iso8601DateFormat = new SimpleDateFormat(iso8601DatePattern);				//"2012-02-08T00:00:00.000+0000"
	protected Calendar parseISO8601DateString(String dateString) {
		return parseISO8601DateString(dateString, null);
	}
	protected Calendar parseISO8601DateString(String dateString, String timeZone) {
		String datePattern="'UNEXPECTED DATE PATTERN'";// = iso8601DatePattern;
		// 12/8/2015 candlepin dropped the milliseconds on the reported date pattern in candlepin commit 16b18540f84443a19786b2f97773e481ecd3c011 https://github.com/candlepin/candlepin/commit/16b18540f84443a19786b2f97773e481ecd3c011
		//   old: "yyyy-MM-dd'T'HH:mm:ss.SSSZ"	// "startDate": "2015-11-09T03:17:36.000+0000"
		//   new: "yyyy-MM-dd'T'HH:mm:ssZ"		// "startDate": "2015-12-07T19:00:00-0500"
		if (dateString.matches("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d[+-]\\d\\d\\d\\d"))	datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
		if (dateString.matches("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d[+-]\\d\\d\\d\\d"))				datePattern = "yyyy-MM-dd'T'HH:mm:ssZ";
		
		if (timeZone==null) datePattern = datePattern.replaceFirst("Z$", "");	// strip off final timezone offset symbol from iso8601DatePattern
		return parseDateStringUsingDatePattern(dateString, datePattern, timeZone);
	}
	protected Calendar parseDateStringUsingDatePattern(String dateString, String datePattern, String timeZone) {
		try{
			DateFormat dateFormat = new SimpleDateFormat(datePattern);	// format="yyyy-MM-dd'T'HH:mm:ss.SSSZ" will parse dateString="2012-02-08T00:00:00.000+0000"
			if (timeZone!=null) dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));	// timeZone="GMT"
			Calendar calendar = new GregorianCalendar();
			if (timeZone!=null) calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
			calendar.setTimeInMillis(dateFormat.parse(dateString).getTime());
			return calendar;
		}
		catch (ParseException e){
			log.warning("Failed to parse "+(timeZone==null?"":timeZone)+" date string '"+dateString+"' with format '"+datePattern+"':\n"+e.getMessage());
			return null;
		}
	}
	protected String formatISO8601DateString(Calendar date) throws UnsupportedEncodingException {
		// for backward compatibility caused by the date pattern change in candlepin commit 16b18540f84443a19786b2f97773e481ecd3c011, let's always use the old pattern
		DateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");				//"2012-02-08T00:00:00.000+0000"
		
		String iso8601FormatedDateString = iso8601DateFormat.format(date.getTime());
		iso8601FormatedDateString = iso8601FormatedDateString.replaceFirst("(..$)", ":$1");		// "2012-02-08T00:00:00.000+00:00"	// see https://bugzilla.redhat.com/show_bug.cgi?id=720493 // http://books.xmlschemata.org/relaxng/ch19-77049.html requires a colon in the time zone for xsd:dateTime
		return iso8601FormatedDateString;
	}
	protected String urlEncode(String formattedDate) throws UnsupportedEncodingException {
		String urlEncodedDate = java.net.URLEncoder.encode(formattedDate, "UTF-8");	// "2012-02-08T00%3A00%3A00.000%2B00%3A00"	encode the string to escape the colons and plus signs so it can be passed as a parameter on an http call
		return urlEncodedDate;
	}
	
	
	protected Map<File,List<Translation>> buildTranslationFileMapForSubscriptionManager() {
		Map<File,List<Translation>> translationFileMapForSubscriptionManager = new HashMap<File, List<Translation>>();
		if (client==null) return translationFileMapForSubscriptionManager;
		
		SSHCommandResult translationFileListingResult = client.runCommandAndWait("rpm -ql subscription-manager | grep rhsm.mo");
		for (String translationFilePath : translationFileListingResult.getStdout().trim().split("\\n")) {
			if (translationFilePath.isEmpty()) continue; // skip empty lines
			
			File translationFile = new File(translationFilePath);
			
			// decompile the rhsm.mo file into its original-like rhsm.po file
			log.info("Decompiling the rhsm.mo file...");
			File translationPoFile = new File(translationFile.getPath().replaceFirst(".mo$", ".po"));
			RemoteFileTasks.runCommandAndAssert(client,"msgunfmt --no-wrap "+translationFile+ " -o "+translationPoFile,new Integer(0));
			
			// parse the translations from the rhsm.mo into the translationFileMap
			SSHCommandResult msgunfmtListingResult = client.runCommandAndWaitWithoutLogging("msgunfmt --no-wrap "+translationFilePath);
			translationFileMapForSubscriptionManager.put(translationFile, Translation.parse(msgunfmtListingResult.getStdout()));
		}
		return translationFileMapForSubscriptionManager;
	}
	
	
	protected Map<File,List<Translation>> buildTranslationFileMapForCandlepin() {
		Map<File,List<Translation>> translationFileMapForCandlepin = new HashMap<File, List<Translation>>();
		if (server==null) return translationFileMapForCandlepin;
		
		//SSHCommandResult translationFileListingResult = server.runCommandAndWait("find "+sm_serverInstallDir+"/po -name *.po");
		SSHCommandResult translationFileListingResult = server.runCommandAndWait("find "+sm_serverInstallDir+" -name *.po");
		for (String translationFilePath : translationFileListingResult.getStdout().trim().split("\\n")) {
			if (translationFilePath.isEmpty()) continue; // skip empty lines
			
			File translationFile = new File(translationFilePath);
					
			// parse the translations from the po file into the translationFileMap
			SSHCommandResult catListingResult = server.runCommandAndWaitWithoutLogging("cat "+translationFilePath);
			translationFileMapForCandlepin.put(translationFile, Translation.parse(catListingResult.getStdout()));
		}
		return translationFileMapForCandlepin;
	}
	
	
	protected void verifyCurrentEntitlementCertsAreReflectedInCurrentlySubscribedYumRepos(List<ProductCert> currentProductCerts) {
		if (currentProductCerts==null) currentProductCerts = clienttasks.getCurrentProductCerts();
		List<YumRepo> yumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
		int numYumReposProvided = 0;
		for (EntitlementCert entitlementCert : entitlementCerts) {
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (contentNamespace.type.equals("yum")) {
					if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
						numYumReposProvided++;
						Assert.assertNotNull(YumRepo.findFirstInstanceWithMatchingFieldFromList("id", contentNamespace.label, yumRepos), "The '"+clienttasks.redhatRepoFile+"' has an entry for contentNamespace label '"+contentNamespace.label+"'.");
					}
				} else {
					// contentNamespace types that are not "yum" should not be included in the redhat.repo file (e.g. "file" and "kickstart")
					Assert.assertNull(YumRepo.findFirstInstanceWithMatchingFieldFromList("id", contentNamespace.label, yumRepos), "The '"+clienttasks.redhatRepoFile+"' should NOT have an entry for contentNamespace label '"+contentNamespace.label+"' because it's type '"+contentNamespace.type+"' is not equal to 'yum'.");				
				}
			}
		}
		if (entitlementCerts.isEmpty()) {
			Assert.assertTrue(yumRepos.isEmpty(),"When there are no entitlement contentNamespaces, then '"+clienttasks.redhatRepoFile+"' should have no yumRepo entries.");
		} else if (numYumReposProvided==0) {
			Assert.assertTrue(yumRepos.isEmpty(),"When none of the currently installed product certs provideTags matching the currently entitled content namespace requiredTags, then '"+clienttasks.redhatRepoFile+"' should have no yumRepo entries.");
		}
	}
	
	
	
	
	
	
	
	// Data Providers ***********************************************************************

	@DataProvider(name="getGoodRegistrationData")
	public Object[][] getGoodRegistrationDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getGoodRegistrationDataAsListOfLists());
	}
	/**
	 * @return List of [String username, String password, String owner]
	 */
	protected List<List<Object>> getGoodRegistrationDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		// parse the registrationDataList to get all the successfully registeredConsumers
		for (RegistrationData registeredConsumer : registrationDataList) {
			if (registeredConsumer.registerResult.getExitCode().intValue()==0) {
				ll.add(Arrays.asList(new Object[]{registeredConsumer.username, registeredConsumer.password, registeredConsumer.ownerKey}));
				
				// minimize the number of dataProvided rows (useful during automated testcase development)
				if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
			}
		}
		
		return ll;
	}
	
	@DataProvider(name="getAllAvailableSubscriptionPoolsData")
	public Object[][] getAllAvailableSubscriptionPoolsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAllAvailableSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAllAvailableSubscriptionPoolsDataAsListOfLists() {
		return getAvailableSubscriptionPoolsDataAsListOfLists(true);
	}
	@DataProvider(name="getAvailableSubscriptionPoolsData")
	public Object[][] getAvailableSubscriptionPoolsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableSubscriptionPoolsDataAsListOfLists() {
		return getAvailableSubscriptionPoolsDataAsListOfLists(false);
	}
	/**
	 * @return a random subset of data rows from getAvailableSubscriptionPoolsDataAsListOfLists() (maximum of 3 rows) (useful to help reduce excessive test execution time)
	 * @throws Exception
	 */
	@DataProvider(name="getRandomSubsetOfAvailableSubscriptionPoolsData")
	public Object[][] getRandomSubsetOfAvailableSubscriptionPoolsDataAs2dArray() throws Exception {
		int subMax = 3;	// maximum subset count of data rows to return
		return TestNGUtils.convertListOfListsTo2dArray(getRandomSubsetOfList(getAvailableSubscriptionPoolsDataAsListOfLists(),subMax));
	}
	
	
	@DataProvider(name="getAllAvailableNonTemporarySubscriptionPoolsData")
	public Object[][] getAllAvailableNonTemporarySubscriptionPoolsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAllAvailableNonTemporarySubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAllAvailableNonTemporarySubscriptionPoolsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getAvailableSubscriptionPoolsDataAsListOfLists(true)) {
			SubscriptionPool p = (SubscriptionPool)l.get(0);
			if (!p.subscriptionType.contains("Temporary")) ll.add(l);
		}
		return ll;
	}
	@DataProvider(name="getAvailableNonTemporarySubscriptionPoolsData")
	public Object[][] getAvailableNonTemporarySubscriptionPoolsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableNonTemporarySubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableNonTemporarySubscriptionPoolsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getAvailableSubscriptionPoolsDataAsListOfLists(false)) {
			SubscriptionPool p = (SubscriptionPool)l.get(0);
			if (!p.subscriptionType.contains("Temporary")) ll.add(l);
		}
		return ll;
	}
	/**
	 * @return a random subset of data rows from getAvailableNonTemporarySubscriptionPoolsDataAsListOfLists() (maximum of 3 rows) (useful to help reduce excessive test execution time)
	 * @throws Exception
	 */
	@DataProvider(name="getRandomSubsetOfAvailableNonTemporarySubscriptionPoolsData")
	public Object[][] getRandomSubsetOfAvailableNonTemporarySubscriptionPoolsDataAs2dArray() throws Exception {
		int subMax = 3;	// maximum subset count of data rows to return
		return TestNGUtils.convertListOfListsTo2dArray(getRandomSubsetOfList(getAvailableNonTemporarySubscriptionPoolsDataAsListOfLists(),subMax));
	}
	
	/**
	 * @return List of [SubscriptionPool pool]
	 */
	protected List<List<Object>> getAvailableSubscriptionPoolsDataAsListOfLists(boolean all) {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		if (sm_clientUsername==null) return ll;
		if (sm_clientPassword==null) return ll;
		
		// assure we are registered
		clienttasks.unregister(null, null, null, null);
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		if (client2tasks!=null)	{
			client2tasks.unregister(null, null, null, null);
			if (!sm_client2Username.equals("") && !sm_client2Password.equals(""))
				client2tasks.register(sm_client2Username, sm_client2Password, sm_client2Org, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		}
		
		// unsubscribe from all consumed product subscriptions and then assemble a list of all SubscriptionPools
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		if (client2tasks!=null)	{
			client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		}

		// populate a list of all available SubscriptionPools
		List<SubscriptionPool> pools = all? clienttasks.getCurrentlyAllAvailableSubscriptionPools():clienttasks.getCurrentlyAvailableSubscriptionPools();
		for (SubscriptionPool pool : pools) {
///*debugTesting bugzilla 1246636*/ if (pool.productId.equals("adminos-onesocketib"))
			ll.add(Arrays.asList(new Object[]{pool}));
			
			// minimize the number of dataProvided rows (useful during automated testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
		}
		
		// manually reorder the pools so that the base "Red Hat Enterprise Linux*" pool is first in the list
		// This is a workaround for testInstallAndRemovePackageAfterSubscribingToPool so as to avoid installing
		// a package from a repo that has a package dependency from a repo that is not yet entitled.
		int i=0;
		for (List<Object> list : ll) {
			if (((SubscriptionPool)(list.get(0))).subscriptionName.startsWith("Red Hat Enterprise Linux")) {
				ll.remove(i);
				ll.add(0, list);
				break;
			}
			i++;
		}
		
		return ll;
	}

	
// DELETEME
//	@DataProvider(name="getUsernameAndPasswordData")
//	public Object[][] getUsernameAndPasswordDataAs2dArray() {
//		return TestNGUtils.convertListOfListsTo2dArray(getUsernameAndPasswordDataAsListOfLists());
//	}
//	protected List<List<Object>> getUsernameAndPasswordDataAsListOfLists() {
//		List<List<Object>> ll = new ArrayList<List<Object>>();
//		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users | python -mjson.tool
//		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users/testuser1 | python -mjson.tool
//		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users/testuser1/owners | python -mjson.tool
//
//		String[] usernames = sm_clientUsernames.split(",");
//		String[] passwords = sm_clientPasswords.split(",");
//		String password = passwords[0].trim();
//		for (int i = 0; i < usernames.length; i++) {
//			String username = usernames[i].trim();
//			// when there is not a 1:1 relationship between usernames and passwords, the last password is repeated
//			// this allows one to specify only one password when all the usernames share the same password
//			if (i<passwords.length) password = passwords[i].trim();
//			
//			// get the orgs for this username/password
//			List<String> orgs = clienttasks.getOrgs(username,password);
//			if (orgs.size()==1) {orgs.clear(); orgs.add(null);}	// 
//			
//			// append a username and password for each org the user belongs to
//			for (String org : orgs) {
//				ll.add(Arrays.asList(new Object[]{username,password,org}));
//			}
//		}
//		
//		return ll;
//	}

	@DataProvider(name="getRegisterCredentialsData")
	public Object[][] getRegisterCredentialsDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterCredentialsDataAsListOfLists());
	}
	@DataProvider(name="getRegisterCredentialsExcludingNullOrgData")
	public Object[][] getRegisterCredentialsExcludingNullOrgDataAs2dArray() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		for (List<Object> l : getRegisterCredentialsDataAsListOfLists()) {
			// l contains: String username, String password, String owner
			if (l.get(2) != null) {
				if (!l.get(2).equals("null")) {
					ll.add(l);
				}
			}
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	/**
	 * @return List of [String username, String password, String org]
	 */
	protected List<List<Object>> getRegisterCredentialsDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// when the candlepin server is not standalone, then we usually don't have access to the candlepin api paths to query the users, so let's use the input parameters 
		if (!sm_serverType.equals(CandlepinType.standalone)) {
			for (String username : sm_clientUsernames) {
				String password = sm_clientPasswordDefault;
			
				// get the orgs for this username/password
				//List<Org> orgs = clienttasks.getOrgs(username,password);	// fails when: You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc
				List<Org> orgs = Org.parse(clienttasks.orgs_(username, password, null, null, null, null, null, null).getStdout());
				//if (orgs.size()==1) {orgs.clear(); orgs.add(new Org(null,null));}	// when a user belongs to only one org, then we don't really need to know the orgKey for registration
				if (orgs.isEmpty()) orgs.add(new Org("null","Null"));	// reveals when: You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc
			
				// append a username and password for each org the user belongs to
				for (Org org : orgs) {
					ll.add(Arrays.asList(new Object[]{username,password,org.orgKey}));
				}
			}
			return ll;
		}
		
				
		// Notes...
		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users | python -mjson.tool
		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users/testuser1 | python -mjson.tool
		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users/testuser1/owners | python -mjson.tool

		// get all of the candlepin users
		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users | python -mjson.tool
		JSONArray jsonUsers = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/users"));	
		for (int i = 0; i < jsonUsers.length(); i++) {
			JSONObject jsonUser = (JSONObject) jsonUsers.get(i);

			// Candlepin Users
			//    {
			//        "created": "2011-09-23T14:42:25.924+0000", 
			//        "hashedPassword": "e3e80f61a902ceca245e22005dffb4219ac1c5f7", 
			//        "id": "8a90f8c63296bc55013296bcc4040005", 
			//        "superAdmin": true, 
			//        "updated": "2011-09-23T14:42:25.924+0000", 
			//        "username": "admin"
			//    }, 
			
			// Katello Users...
			//    {
			//        "created_at": "2011-09-24T01:29:02Z", 
			//        "disabled": false, 
			//        "helptips_enabled": true, 
			//        "id": 1, 
			//        "own_role_id": 4, 
			//        "page_size": 25, 
			//        "password": "07a1dacc4f283e817c0ba353bd1452de49ce5723b2b7f56f6ee2f1f400a974b360f98acb90b630c7fa411f692bdb4c5cdd0f4b916efcf3c77e7cd0453446b185TS0YtS0uRjY0UznEsx7JqGIpEM1vEfIrBSNGdnXkFdkxsDhjmyFINBJVvkCTxeC7", 
			//        "updated_at": "2011-09-24T01:29:02Z", 
			//        "username": "admin"
			//    }, 
			
			//Boolean isSuperAdmin = jsonUser.getBoolean("superAdmin");
			String username = jsonUser.getString("username");
			String password = sm_clientPasswordDefault;
			if (username.equals(sm_serverAdminUsername)) password = sm_serverAdminPassword;
			
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=741961 - jsefler 9/29/2011
			if (username.equals("anonymous")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="741961"; 
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring the presence of user '"+username+"'.  No automated testing with this user will be executed.");
					continue;
				}
			}
			// END OF WORKAROUND
			
			// get the user's owners
			// curl -k -u testuser1:password https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users/testuser1/owners | python -mjson.tool
			JSONArray jsonUserOwners = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(username,password,sm_serverUrl,"/users/"+username+"/owners"));	
			for (int j = 0; j < jsonUserOwners.length(); j++) {
				JSONObject jsonOwner = (JSONObject) jsonUserOwners.get(j);
				// {
				//    "contentPrefix": null, 
				//    "created": "2011-07-01T06:39:58.740+0000", 
				//    "displayName": "Snow White", 
				//    "href": "/owners/snowwhite", 
				//    "id": "8a90f8c630e46c7e0130e46ce114000a", 
				//    "key": "snowwhite", 
				//    "parentOwner": null, 
				//    "updated": "2011-07-01T06:39:58.740+0000", 
				//    "upstreamUuid": null
				// }
				String owner = jsonOwner.getString("key");
				
				// String username, String password, String owner
				ll.add(Arrays.asList(new Object[]{username,password,owner}));
			}
			
			// don't forget that some users (for which no owners are returned) probably have READ_ONLY permission to their orgs
			if (jsonUserOwners.length()==0) {
				ll.add(Arrays.asList(new Object[]{username,password,null}));			
			}
			
			// minimize the number of dataProvided rows (useful during automated testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
		}
		
		return ll;
	}
	
	@DataProvider(name="getRandomSubsetOfConsumedProductSubscriptionsData")
	public Object[][] getRandomSubsetOfConsumedProductSubscriptionsDataAs2dArray() throws JSONException, Exception {
		int subsetSize = 10;	// maximum subset count of data rows to return
		return TestNGUtils.convertListOfListsTo2dArray(getRandomSubsetOfList(getAllConsumedProductSubscriptionsDataAsListOfLists(),subsetSize));
	}
	@DataProvider(name="getAllConsumedProductSubscriptionsData")
	public Object[][] getAllConsumedProductSubscriptionsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllConsumedProductSubscriptionsDataAsListOfLists());
	}
	/**
	 * @return List of [ProductSubscription productSubscription]
	 */
	protected List<List<Object>> getAllConsumedProductSubscriptionsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		if (sm_clientUsername==null) return ll;
		if (sm_clientPassword==null) return ll;
		
		// first make sure we are subscribed to all pools
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		
		// then assemble a list of all consumed ProductSubscriptions
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			ll.add(Arrays.asList(new Object[]{productSubscription}));
			
			// minimize the number of dataProvided rows (useful during automated testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
		}
		
		return ll;
	}
	
	
	@DataProvider(name="getAllEntitlementCertsData")
	public Object[][] getAllEntitlementCertsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllEntitlementCertsDataAsListOfLists());
	}
	/**
	 * @return List of [EntitlementCert entitlementCert]
	 */
	protected List<List<Object>> getAllEntitlementCertsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		if (sm_clientUsername==null) return ll;
		if (sm_clientPassword==null) return ll;
		
		// first make sure we are subscribed to all pools
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();

		
		// then assemble a list of all consumed ProductSubscriptions
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
			ll.add(Arrays.asList(new Object[]{entitlementCert}));
			
			// minimize the number of dataProvided rows (useful during automated testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
		}
		
		return ll;
	}
	
	
	@DataProvider(name="getAllSystemSubscriptionPoolProductData")
	public Object[][] getAllAvailableSystemSubscriptionPoolProductDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getSystemSubscriptionPoolProductDataAsListOfLists(false, false));
	}
	@DataProvider(name="getAvailableSystemSubscriptionPoolProductData")
	public Object[][] getAvailableSystemSubscriptionPoolProductDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getSystemSubscriptionPoolProductDataAsListOfLists(true, false));
	}
	/**
	 * @param matchSystemHardware - make sure that atLeastOneProvidedProductSatisfiesArch
	 * @param matchSystemSoftware - makes sure that atLeastOneProvidedProductIsInstalled
	 * @return
	 * @throws Exception
	 */
	protected List<List<Object>> getSystemSubscriptionPoolProductDataAsListOfLists(boolean matchSystemHardware, boolean matchSystemSoftware) throws Exception {
		JSONObject jsonStatus = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(/*authenticator*/null,/*password*/null,sm_serverUrl,"/status"));
		
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		List <String> productIdsAddedToSystemSubscriptionPoolProductData = new ArrayList<String>();
		
		// is this system virtual
		boolean isSystemVirtual = Boolean.valueOf(clienttasks.getFactValue("virt.is_guest"));
		
		// get the owner key for clientusername, clientpassword
		String consumerId = clienttasks.getCurrentConsumerId();
		if (consumerId==null) consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null));
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
		// java.lang.RuntimeException: org.json.JSONException: JSONObject["owner"] not found.
		// ^^^ this will be thrown when the consumerId has been deleted at the server, but the client does not know it.
		
		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		
		// get the currently installed product certs
		List<ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		
		// process all of the subscriptions/pools belonging to ownerKey...
		
		// decide whether to loop through the subscription objects or pool objects
		JSONArray jsonSubs;
		if (SubscriptionManagerTasks.isVersion(jsonStatus.getString("version"), ">=", "2.1.1-1")) {	// candlepin commit 9c448315c843c0a20167236af7591359d895613a Discontinue ambiguous subscription resources in sharing world
			jsonSubs = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+ownerKey+"/pools?add_future=true"));	// /pools?add_future=true&include=productId;
		} else {
			/* 7/10/2015 devel consciously decided to drop @Verify(value = Owner.class, subResource = SubResource.SUBSCRIPTIONS) on this GET method starting with candlepin-2.0.
			 * 7/10/2015 modifying this testware to simply raise the authentication credentials to admin
			jsonSubs = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));
			 */
			jsonSubs = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));;
		}
		
		// loop through all of the jsonSubs
		for (int i = 0; i < jsonSubs.length(); i++) {
			JSONObject jsonSub = (JSONObject) jsonSubs.get(i);
			
			// skip derived pools
			if (jsonSub.has("subscriptionSubKey") && !jsonSub.isNull("subscriptionSubKey")) {	// null (custom pools not originating from a subscription)
				if (jsonSub.getString("subscriptionSubKey").toLowerCase().equals("derived")) {	// master, derived, 
					continue;
				}
			}
			
			// skip future subscriptions that are not valid today (at this time now)
			Calendar startDate = parseISO8601DateString(jsonSub.getString("startDate"),"GMT");	// "startDate":"2012-02-08T00:00:00.000+0000"
			Calendar endDate = parseISO8601DateString(jsonSub.getString("endDate"),"GMT");	// "endDate":"2013-02-07T00:00:00.000+0000"
			if (!(startDate.before(now) && endDate.after(now))) continue;
			
			// get the product id name and attributes
			JSONArray jsonProductAttributes;
			String productId;
			String productName;
			if (jsonSub.has("product")) {	// when jsonSub comes from /owners/{ownerKey}/subscriptions
				productId = jsonSub.getJSONObject("product").getString("id");
				productName = jsonSub.getJSONObject("product").getString("name");
				jsonProductAttributes = jsonSub.getJSONObject("product").getJSONArray("attributes");
			} else {	// when jsonSub comes from /owners/{ownerKey}/pools
				productId = jsonSub.getString("productId");
				productName = jsonSub.getString("productName");
				jsonProductAttributes = jsonSub.getJSONArray("productAttributes");
			}
			
			// skip subscriptions that have already been added to SystemSubscriptionPoolProductData
			if (productIdsAddedToSystemSubscriptionPoolProductData.contains(productId)) continue;
			
			// process this subscription productId
			boolean productAttributesPassRulesCheck = true; // assumed
			String productAttributeSocketsValue = "";
			List<String> productSupportedArches = new ArrayList<String>();
			String productAttributeStackingIdValue = null;
			for (int j = 0; j < jsonProductAttributes.length(); j++) {	// loop product attributes to find a stacking_id
				if (((JSONObject) jsonProductAttributes.get(j)).getString("name").equals("stacking_id")) {
					productAttributeStackingIdValue = ((JSONObject) jsonProductAttributes.get(j)).getString("value");
					break;
				}
			}
			for (int j = 0; j < jsonProductAttributes.length(); j++) {
				JSONObject jsonProductAttribute = (JSONObject) jsonProductAttributes.get(j);
				String attributeName = jsonProductAttribute.getString("name");
				//String attributeValue = jsonProductAttribute.getString("value");
				String attributeValue = jsonProductAttribute.isNull("value")? null:jsonProductAttribute.getString("value");
				if (attributeName.equals("arch")) {
					productSupportedArches.addAll(Arrays.asList(attributeValue.trim().split(" *, *")));	// Note: the arch attribute can be a comma separated list of values
					if (productSupportedArches.contains("x86")) {productSupportedArches.addAll(Arrays.asList("i386","i486","i586","i686"));}  // Note: x86 is a general arch to cover all 32-bit intel microprocessors 
					if (!productSupportedArches.contains("ALL") && !productSupportedArches.contains(clienttasks.arch)) {
						productAttributesPassRulesCheck = false;
					}
				}
				if (attributeName.equals("variant")) {
//					if (!attributeValue.equalsIgnoreCase("ALL") && !attributeValue.equalsIgnoreCase(clienttasks.variant)) {
//						productAttributesPassRulesCheck = false;
//					}
				}
				if (attributeName.equals("type")) {

				}
				if (attributeName.equals("multi-entitlement")) {

				}
				if (attributeName.equals("warning_period")) {

				}
				if (attributeName.equals("version")) {
//					if (!attributeValue.equalsIgnoreCase(clienttasks.version)) {
//						productAttributesPassRulesCheck = false;
//					}
				}
				if (attributeName.equals("requires_consumer_type")) {
					if (!attributeValue.equalsIgnoreCase(ConsumerType.system.toString())) {
						productAttributesPassRulesCheck = false;
					}
				}
				if (attributeName.equals("stacking_id")) {
					// productAttributeStackingIdValue = attributeValue; // was already searched for above
				}
				if (attributeName.equals("physical_only")) {
					if (attributeValue!=null && Boolean.valueOf(attributeValue) && isSystemVirtual) {
						productAttributesPassRulesCheck = false;
					}
				}
				if (attributeName.equals("virt_only")) {
					if (attributeValue!=null && Boolean.valueOf(attributeValue) && !isSystemVirtual) {
						productAttributesPassRulesCheck = false;
					}
				}
				if (attributeName.equals("sockets")) {
					productAttributeSocketsValue = attributeValue;
					
					// if this subscription is stackable (indicated by the presence of a stacking_id attribute)
					// then there is no need to check the system's sockets to see if this subscription should be available 
					// Because this subscription is stackable, it better not be filtered out from availability based on the system's sockets.
					if (productAttributeStackingIdValue==null) {
						
						SOCKETS:
						{
							// if the sockets attribute is not numeric (e.g. null or "unlimited"),  then this subscription should be available to this client
							try {Integer.valueOf(attributeValue);}
							catch (NumberFormatException e) {
								// do not mark productAttributesPassRulesCheck = false;
								log.warning("THE VALIDITY OF SUBSCRIPTION productName='"+productName+"' productId='"+productId+"' IS QUESTIONABLE.  IT HAS A '"+attributeName+"' PRODUCT ATTRIBUTE OF '"+attributeValue+"' WHICH IS NOT NUMERIC.  SIMPLY IGNORING THIS ATTRIBUTE.");
								break SOCKETS;
							}
							
							// business rule is if this client is virtual, then we do not consider sockets at all.
							// Example: a 1 socket physical or virtual subscription pool should be available for consumption my a 2 socket virt guest 
							if (isSystemVirtual) {
								// do not mark productAttributesPassRulesCheck = false;
								break SOCKETS;
							}
							
							// if the sockets attribute is zero, then this subscription should be available to this client
							if (Integer.valueOf(attributeValue)==0) {
								// do not mark productAttributesPassRulesCheck = false;
							} else
							
							// if the socket count on this client exceeds the sockets attribute, then this subscription should NOT be available to this client
							if (Integer.valueOf(attributeValue) < Integer.valueOf(clienttasks.sockets)) {
								if (matchSystemHardware) productAttributesPassRulesCheck = false;
							}
						}
					}
				}
			}
			if (productAttributesPassRulesCheck) {
				
				// process this subscription's providedProducts
				Boolean atLeastOneProvidedProductIsInstalled = false; // assumed
				Boolean atLeastOneProvidedProductSatisfiesArch = false; // assumed
				JSONArray jsonBundledProductData = new JSONArray();
				JSONArray jsonProvidedProducts = (JSONArray) jsonSub.getJSONArray("providedProducts");
				if (jsonProvidedProducts.length()==0) atLeastOneProvidedProductIsInstalled = true;	// effectively true when no provided products are installed
				if (jsonProvidedProducts.length()==0) atLeastOneProvidedProductSatisfiesArch = true;	// effectively true when no provided products are installed
				for (int k = 0; k < jsonProvidedProducts.length(); k++) {
					
					String providedProductId;
					if (((JSONObject) jsonProvidedProducts.get(k)).has("id")) {	// when jsonSub comes from /owners/{ownerKey}/subscriptions
						providedProductId = ((JSONObject) jsonProvidedProducts.get(k)).getString("id");
					} else {	// when jsonSub comes from /owners/{ownerKey}/pools
						providedProductId = ((JSONObject) jsonProvidedProducts.get(k)).getString("productId");
					}
					JSONObject jsonProvidedProduct = new JSONObject (CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+ownerKey+"/products/"+providedProductId));
					String providedProductName = jsonProvidedProduct.getString("name");
					
					// process this providedProducts attributes
					JSONArray jsonProvidedProductAttributes = jsonProvidedProduct.getJSONArray("attributes");
					boolean providedProductAttributesPassRulesCheck = true; // assumed
					for (int l = 0; l < jsonProvidedProductAttributes.length(); l++) {
						JSONObject jsonProvidedProductAttribute = (JSONObject) jsonProvidedProductAttributes.get(l);
						String attributeName = jsonProvidedProductAttribute.getString("name");
						//String attributeValue = jsonProvidedProductAttribute.getString("value");
						String attributeValue = jsonProvidedProductAttribute.isNull("value")? null:jsonProvidedProductAttribute.getString("value");
						/* 6/17/2011 The availability of a Subscription depends only on its attributes and NOT the attributes of its provided products.
						 * You will get ALL of its provided product even if they don't make arch/socket sense.
						 * In this case you could argue that it is not subscription-manager's job to question the meaningfulness of the subscription and its provided products.
						 * For this reason, I am commenting out all the providedProductAttributesPassRulesCheck = false; ... (except "type")
						 */
						if (attributeName.equals("arch")) {
							List<String> supportedArches = new ArrayList<String>(Arrays.asList(attributeValue.trim().split(" *, *")));	// Note: the arch attribute can be a comma separated list of values
							if (supportedArches.contains("x86")) {supportedArches.addAll(Arrays.asList("i386","i486","i586","i686"));}  // Note: x86 is a general term to cover all 32-bit intel microprocessors 
							if (!productSupportedArches.isEmpty() && !productSupportedArches.containsAll(supportedArches)) {
								log.warning("THE VALIDITY OF SUBSCRIPTION productName='"+productName+"' productId='"+productId+"' WITH PROVIDED PRODUCT '"+providedProductName+"' IS QUESTIONABLE.  THE PROVIDED PRODUCT '"+providedProductId+"' ARCH ATTRIBUTE '"+attributeValue+"' IS NOT A SUBSET OF THE TOP LEVEL SUBSCRIPTION PRODUCT '"+productId+"' ARCH ATTRIBUTE '"+productSupportedArches+"'.");
							}
							if (!supportedArches.contains("ALL") && !supportedArches.contains(clienttasks.arch)) {
								//providedProductAttributesPassRulesCheck = false;
							} else {
								atLeastOneProvidedProductSatisfiesArch = true;
							}
						}
						if (attributeName.equals("variant")) {
//								if (!attributeValue.equalsIgnoreCase("ALL") && !attributeValue.equalsIgnoreCase(clienttasks.variant)) {
//									providedProductAttributesPassRulesCheck = false;
//								}
						}
						if (attributeName.equals("type")) {
							if (attributeValue.equals("MKT")) { // provided products of type "MKT" should not pass the rules check  e.g. providedProductName="Awesome OS Server Bundled"
								providedProductAttributesPassRulesCheck = false;	// do not comment out!
							}
						}
						if (attributeName.equals("version")) {
//								if (!attributeValue.equalsIgnoreCase(clienttasks.version)) {
//									providedProductAttributesPassRulesCheck = false;
//								}
						}
						if (attributeName.equals("requires_consumer_type")) {
							if (!attributeValue.equalsIgnoreCase(ConsumerType.system.toString())) {
								//providedProductAttributesPassRulesCheck = false;
							}
						}
						if (attributeName.equals("sockets")) {
//							if (!attributeValue.equals(productAttributeSocketsValue)) {
//								log.warning("THE VALIDITY OF SUBSCRIPTION productName='"+productName+"' productId='"+productId+"' WITH PROVIDED PRODUCT '"+providedProductName+"' IS QUESTIONABLE.  THE PROVIDED PRODUCT '"+providedProductId+"' SOCKETS ATTRIBUTE '"+attributeValue+"' DOES NOT MATCH THE TOP LEVEL SUBSCRIPTION PRODUCT '"+productId+"' SOCKETS ATTRIBUTE '"+productAttributeSocketsValue+"'.");
//							}
//							if (!productAttributeSocketsValue.equals("") && Integer.valueOf(attributeValue) > Integer.valueOf(productAttributeSocketsValue)) {
//								//providedProductAttributesPassRulesCheck = false;
//							}
						}
					}
					if (providedProductAttributesPassRulesCheck) {
						JSONObject bundledProduct = new JSONObject(String.format("{productName:'%s', productId:'%s'}", providedProductName,providedProductId));

						jsonBundledProductData.put(bundledProduct);
					}
					
					
					// is this provided product already installed
					if (ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, productCerts) != null) atLeastOneProvidedProductIsInstalled=true;
					
				}
				
				
				// Example:
				// < {systemProductId:'awesomeos-modifier', bundledProductData:<{productName:'Awesome OS Modifier Bits'}>} , {systemProductId:'awesomeos-server', bundledProductData:<{productName:'Awesome OS Server Bits'},{productName:'Clustering Bits'},{productName:'Shared Storage Bits'},{productName:'Management Bits'},{productName:'Large File Support Bits'},{productName:'Load Balancing Bits'}>} , {systemProductId:'awesomeos-server-basic', bundledProductData:<{productName:'Awesome OS Server Bits'}>} , {systemProductId:'awesomeos-workstation-basic', bundledProductData:<{productName:'Awesome OS Workstation Bits'}>} , {systemProductId:'awesomeos-server-2-socket-std', bundledProductData:<{productName:'Awesome OS Server Bits'},{productName:'Clustering Bits'},{productName:'Shared Storage Bits'},{productName:'Management Bits'},{productName:'Large File Support Bits'},{productName:'Load Balancing Bits'}>} , {systemProductId:'awesomeos-virt-4', bundledProductData:<{productName:'Awesome OS Server Bits'}>} , {systemProductId:'awesomeos-server-2-socket-prem', bundledProductData:<{productName:'Awesome OS Server Bits'},{productName:'Clustering Bits'},{productName:'Shared Storage Bits'},{productName:'Management Bits'},{productName:'Large File Support Bits'},{productName:'Load Balancing Bits'}>} , {systemProductId:'awesomeos-virt-4', bundledProductData:<{productName:'Awesome OS Server Bits'}>} , {systemProductId:'awesomeos-server-4-socket-prem',bundledProductData:<{productName:'Awesome OS Server Bits'},{productName:'Clustering Bits'},{productName:'Shared Storage Bits'},{productName:'Management Bits'},{productName:'Large File Support Bits'},{productName:'Load Balancing Bits'}>} , {systemProductId:'awesomeos-virt-4', bundledProductData:<{productName:'Awesome OS Server Bits'}>} , {systemProductId:'awesomeos-server-2-socket-bas', bundledProductData:<{productName:'Awesome OS Server Bits'},{productName:'Clustering Bits'},{productName:'Shared Storage Bits'},{productName:'Management Bits'},{productName:'Large File Support Bits'},{productName:'Load Balancing Bits'}>} , {systemProductId:'awesomeos-virt-4', bundledProductData:<{productName:'Awesome OS Server Bits'}>} , {systemProductId:'management-100', bundledProductData:<{productName:'Management Add-On'}>} , {systemProductId:'awesomeos-scalable-fs', bundledProductData:<{productName:'Awesome OS Scalable Filesystem Bits'}>}>
				// String systemProductId, JSONArray bundledProductDataAsJSONArray
				if (matchSystemSoftware && atLeastOneProvidedProductIsInstalled) {
					ll.add(Arrays.asList(new Object[]{productId, jsonBundledProductData}));
					productIdsAddedToSystemSubscriptionPoolProductData.add(productId);
				} else if (matchSystemHardware && atLeastOneProvidedProductSatisfiesArch) {
					ll.add(Arrays.asList(new Object[]{productId, jsonBundledProductData}));
					productIdsAddedToSystemSubscriptionPoolProductData.add(productId);		
				} else if (!matchSystemSoftware && !matchSystemHardware) {
					ll.add(Arrays.asList(new Object[]{productId, jsonBundledProductData}));
					productIdsAddedToSystemSubscriptionPoolProductData.add(productId);		
				}
			}
		}
		
		// minimize the number of dataProvided rows (useful during automated testcase development)
		// WARNING: When true, this will fail the testVerifyNormalAvailablePoolsFromSubscriptionsPassTheHardwareRulesCheck
		if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) ll=ll.subList(0,1);

		return ll;
		
	}
	
	/**
	 * TODO: 4/30/2014 This DataProvider is flawed.  Do not use.  It should be based on poolIds, not productIds.  Because a physical_only pool with a virt_limit can create a BONUS pool, the productId can be available to both physical and virtual systems under two different poolIds.  e.g. "productId": "awesomeos-virt-unlmtd-phys" from TESTDATA
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	@DataProvider(name="getNonAvailableSystemSubscriptionPoolProductData")
	public Object[][] getNonAvailableSystemSubscriptionPoolProductDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getNonAvailableSystemSubscriptionPoolProductDataAsListOfLists());
	}
	/**
	 * TODO: 4/30/2014 This DataProvider is flawed.  Do not use.  It should be based on poolIds, not productIds.  Because a physical_only pool with a virt_limit can create a BONUS pool, the productId can be available to both physical and virtual systems under two different poolIds.  e.g. "productId": "awesomeos-virt-unlmtd-phys" from TESTDATA
	 * @return List of [String productId]
	 */
	@Deprecated
	protected List<List<Object>> getNonAvailableSystemSubscriptionPoolProductDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		List <String> productIdsAddedToNonAvailableSystemSubscriptionPoolProductData = new ArrayList<String>();

		// String systemProductId, JSONArray bundledProductDataAsJSONArray
		List<List<Object>> availSystemSubscriptionPoolProductData = getSystemSubscriptionPoolProductDataAsListOfLists(true,false);
		
		// get the owner key for clientusername, clientpassword
		String consumerId = clienttasks.getCurrentConsumerId();
		if (consumerId==null) consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null));
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);

		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		
		// process all of the subscriptions belonging to ownerKey
		/* 7/10/2015 devel consciously decided to drop @Verify(value = Owner.class, subResource = SubResource.SUBSCRIPTIONS) on this GET method starting with candlepin-2.0.
		 * 7/10/2015 modifying this testware to simply raise the authentication credentials to admin
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));
		 */
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));
		for (int i = 0; i < jsonSubscriptions.length(); i++) {
			JSONObject jsonSubscription = (JSONObject) jsonSubscriptions.get(i);
			
			JSONObject jsonProduct = (JSONObject) jsonSubscription.getJSONObject("product");
			String productId = jsonProduct.getString("id");
			String productName = jsonProduct.getString("name");
			
			// skip subscriptions that have already been added to NonAvailableSystemSubscriptionPoolProductData
			if (productIdsAddedToNonAvailableSystemSubscriptionPoolProductData.contains(productId)) continue;

			boolean isAvailable = false;
			for (List<Object> systemSubscriptionPoolProductDatum : availSystemSubscriptionPoolProductData) {
				String availProductId = (String) systemSubscriptionPoolProductDatum.get(0);
				JSONArray availJsonBundledProductData = (JSONArray) systemSubscriptionPoolProductDatum.get(1);
				if (availProductId.equals(productId)) {
					isAvailable = true;
					break;
				}
			}
			if (!isAvailable) {
				// String systemProductId
				ll.add(Arrays.asList(new Object[]{productId}));
				productIdsAddedToNonAvailableSystemSubscriptionPoolProductData.add(productId);
			}
		}
		return ll;
	}
	
	/* SUBSCRIPTION WITH BUNDLED PRODUCTS
	
	[root@jsefler-onprem-server ~]# curl -k -u admin:admin --request GET https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/subscriptions/8a90f8b42ee62404012ee624918b00a9 | json_reformat 
		  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
		                                 Dload  Upload   Total   Spent    Left  Speed
		100 13941    0 13941    0     0   127k      0 --:--:-- --:--:-- --:--:--  412k
		{
		  "id": "8a90f8b42ee62404012ee624918b00a9",
		  "owner": {
		    "href": "/owners/admin",
		    "id": "8a90f8b42ee62404012ee62448260005"
		  },
		  "certificate": null,
		  "product": {
		    "name": "Awesome OS Server Bundled (2 Sockets, Standard Support)",
		    "id": "awesomeos-server-2-socket-std",
		    "attributes": [
		      {
		        "name": "variant",
		        "value": "ALL",
		        "updated": "2011-03-24T04:34:39.173+0000",
		        "created": "2011-03-24T04:34:39.173+0000"
		      },
		      {
		        "name": "sockets",
		        "value": "2",
		        "updated": "2011-03-24T04:34:39.174+0000",
		        "created": "2011-03-24T04:34:39.174+0000"
		      },
		      {
		        "name": "arch",
		        "value": "ALL",
		        "updated": "2011-03-24T04:34:39.174+0000",
		        "created": "2011-03-24T04:34:39.174+0000"
		      },
		      {
		        "name": "support_level",
		        "value": "Standard",
		        "updated": "2011-03-24T04:34:39.174+0000",
		        "created": "2011-03-24T04:34:39.174+0000"
		      },
		      {
		        "name": "support_type",
		        "value": "L1-L3",
		        "updated": "2011-03-24T04:34:39.175+0000",
		        "created": "2011-03-24T04:34:39.175+0000"
		      },
		      {
		        "name": "management_enabled",
		        "value": "1",
		        "updated": "2011-03-24T04:34:39.175+0000",
		        "created": "2011-03-24T04:34:39.175+0000"
		      },
		      {
		        "name": "type",
		        "value": "MKT",
		        "updated": "2011-03-24T04:34:39.175+0000",
		        "created": "2011-03-24T04:34:39.175+0000"
		      },
		      {
		        "name": "warning_period",
		        "value": "30",
		        "updated": "2011-03-24T04:34:39.176+0000",
		        "created": "2011-03-24T04:34:39.176+0000"
		      },
		      {
		        "name": "version",
		        "value": "6.1",
		        "updated": "2011-03-24T04:34:39.176+0000",
		        "created": "2011-03-24T04:34:39.176+0000"
		      }
		    ],
		    "multiplier": 1,
		    "productContent": [

		    ],
		    "dependentProductIds": [

		    ],
		    "href": "/products/awesomeos-server-2-socket-std",
		    "updated": "2011-03-24T04:34:39.173+0000",
		    "created": "2011-03-24T04:34:39.173+0000"
		  },
		  "providedProducts": [
		    {
		      "name": "Clustering Bits",
		      "id": "37065",
		      "attributes": [
		        {
		          "name": "version",
		          "value": "1.0",
		          "updated": "2011-03-24T04:34:26.104+0000",
		          "created": "2011-03-24T04:34:26.104+0000"
		        },
		        {
		          "name": "variant",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:26.104+0000",
		          "created": "2011-03-24T04:34:26.104+0000"
		        },
		        {
		          "name": "sockets",
		          "value": "2",
		          "updated": "2011-03-24T04:34:26.104+0000",
		          "created": "2011-03-24T04:34:26.104+0000"
		        },
		        {
		          "name": "arch",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:26.104+0000",
		          "created": "2011-03-24T04:34:26.104+0000"
		        },
		        {
		          "name": "type",
		          "value": "SVC",
		          "updated": "2011-03-24T04:34:26.104+0000",
		          "created": "2011-03-24T04:34:26.104+0000"
		        }
		      ],
		      "multiplier": 1,
		      "productContent": [
		        {
		          "content": {
		            "name": "always-enabled-content",
		            "id": "1",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/always",
		            "label": "always-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/always/gpg",
		            "metadataExpire": 200,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.415+0000",
		            "created": "2011-03-24T04:34:25.415+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": true
		        },
		        {
		          "content": {
		            "name": "never-enabled-content",
		            "id": "0",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/never",
		            "label": "never-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/never/gpg",
		            "metadataExpire": 600,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.277+0000",
		            "created": "2011-03-24T04:34:25.277+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": false
		        }
		      ],
		      "dependentProductIds": [

		      ],
		      "href": "/products/37065",
		      "updated": "2011-03-24T04:34:26.103+0000",
		      "created": "2011-03-24T04:34:26.103+0000"
		    },
		    {
		      "name": "Awesome OS Server Bundled",
		      "id": "awesomeos-server",
		      "attributes": [
		        {
		          "name": "version",
		          "value": "1.0",
		          "updated": "2011-03-24T04:34:35.841+0000",
		          "created": "2011-03-24T04:34:35.841+0000"
		        },
		        {
		          "name": "variant",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:35.841+0000",
		          "created": "2011-03-24T04:34:35.841+0000"
		        },
		        {
		          "name": "support_level",
		          "value": "Premium",
		          "updated": "2011-03-24T04:34:35.841+0000",
		          "created": "2011-03-24T04:34:35.841+0000"
		        },
		        {
		          "name": "sockets",
		          "value": "2",
		          "updated": "2011-03-24T04:34:35.841+0000",
		          "created": "2011-03-24T04:34:35.841+0000"
		        },
		        {
		          "name": "arch",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:35.841+0000",
		          "created": "2011-03-24T04:34:35.841+0000"
		        },
		        {
		          "name": "management_enabled",
		          "value": "1",
		          "updated": "2011-03-24T04:34:35.842+0000",
		          "created": "2011-03-24T04:34:35.842+0000"
		        },
		        {
		          "name": "type",
		          "value": "MKT",
		          "updated": "2011-03-24T04:34:35.842+0000",
		          "created": "2011-03-24T04:34:35.842+0000"
		        },
		        {
		          "name": "warning_period",
		          "value": "30",
		          "updated": "2011-03-24T04:34:35.842+0000",
		          "created": "2011-03-24T04:34:35.842+0000"
		        },
		        {
		          "name": "support_type",
		          "value": "Level 3",
		          "updated": "2011-03-24T04:34:35.842+0000",
		          "created": "2011-03-24T04:34:35.842+0000"
		        }
		      ],
		      "multiplier": 1,
		      "productContent": [

		      ],
		      "dependentProductIds": [

		      ],
		      "href": "/products/awesomeos-server",
		      "updated": "2011-03-24T04:34:35.841+0000",
		      "created": "2011-03-24T04:34:35.841+0000"
		    },
		    {
		      "name": "Awesome OS Server Bits",
		      "id": "37060",
		      "attributes": [
		        {
		          "name": "variant",
		          "value": "ALL",
		          "updated": "2011-03-24T05:28:28.464+0000",
		          "created": "2011-03-24T05:28:28.464+0000"
		        },
		        {
		          "name": "sockets",
		          "value": "2",
		          "updated": "2011-03-24T05:28:28.465+0000",
		          "created": "2011-03-24T05:28:28.465+0000"
		        },
		        {
		          "name": "arch",
		          "value": "ALL",
		          "updated": "2011-03-24T05:28:28.464+0000",
		          "created": "2011-03-24T05:28:28.464+0000"
		        },
		        {
		          "name": "type",
		          "value": "SVC",
		          "updated": "2011-03-24T05:28:28.465+0000",
		          "created": "2011-03-24T05:28:28.465+0000"
		        },
		        {
		          "name": "warning_period",
		          "value": "30",
		          "updated": "2011-03-24T05:28:28.465+0000",
		          "created": "2011-03-24T05:28:28.465+0000"
		        },
		        {
		          "name": "version",
		          "value": "6.1",
		          "updated": "2011-03-24T05:28:28.465+0000",
		          "created": "2011-03-24T05:28:28.465+0000"
		        }
		      ],
		      "multiplier": 1,
		      "productContent": [
		        {
		          "content": {
		            "name": "tagged-content",
		            "id": "2",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/always",
		            "label": "tagged-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/always/gpg",
		            "metadataExpire": null,
		            "requiredTags": "TAG1,TAG2",
		            "updated": "2011-03-24T04:34:25.482+0000",
		            "created": "2011-03-24T04:34:25.482+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": true
		        },
		        {
		          "content": {
		            "name": "always-enabled-content",
		            "id": "1",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/always",
		            "label": "always-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/always/gpg",
		            "metadataExpire": 200,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.415+0000",
		            "created": "2011-03-24T04:34:25.415+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": true
		        },
		        {
		          "content": {
		            "name": "never-enabled-content",
		            "id": "0",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/never",
		            "label": "never-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/never/gpg",
		            "metadataExpire": 600,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.277+0000",
		            "created": "2011-03-24T04:34:25.277+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": false
		        },
		        {
		          "content": {
		            "name": "content",
		            "id": "1111",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path",
		            "label": "content-label",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/gpg/",
		            "metadataExpire": 0,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.559+0000",
		            "created": "2011-03-24T04:34:25.559+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": true
		        }
		      ],
		      "dependentProductIds": [

		      ],
		      "href": "/products/37060",
		      "updated": "2011-03-24T04:34:32.608+0000",
		      "created": "2011-03-24T04:34:32.608+0000"
		    },
		    {
		      "name": "Load Balancing Bits",
		      "id": "37070",
		      "attributes": [
		        {
		          "name": "version",
		          "value": "1.0",
		          "updated": "2011-03-24T04:34:27.252+0000",
		          "created": "2011-03-24T04:34:27.252+0000"
		        },
		        {
		          "name": "variant",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:27.252+0000",
		          "created": "2011-03-24T04:34:27.252+0000"
		        },
		        {
		          "name": "sockets",
		          "value": "2",
		          "updated": "2011-03-24T04:34:27.253+0000",
		          "created": "2011-03-24T04:34:27.253+0000"
		        },
		        {
		          "name": "arch",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:27.252+0000",
		          "created": "2011-03-24T04:34:27.252+0000"
		        },
		        {
		          "name": "type",
		          "value": "SVC",
		          "updated": "2011-03-24T04:34:27.253+0000",
		          "created": "2011-03-24T04:34:27.253+0000"
		        },
		        {
		          "name": "warning_period",
		          "value": "30",
		          "updated": "2011-03-24T04:34:27.253+0000",
		          "created": "2011-03-24T04:34:27.253+0000"
		        }
		      ],
		      "multiplier": 1,
		      "productContent": [
		        {
		          "content": {
		            "name": "always-enabled-content",
		            "id": "1",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/always",
		            "label": "always-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/always/gpg",
		            "metadataExpire": 200,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.415+0000",
		            "created": "2011-03-24T04:34:25.415+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": true
		        },
		        {
		          "content": {
		            "name": "never-enabled-content",
		            "id": "0",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/never",
		            "label": "never-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/never/gpg",
		            "metadataExpire": 600,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.277+0000",
		            "created": "2011-03-24T04:34:25.277+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": false
		        }
		      ],
		      "dependentProductIds": [

		      ],
		      "href": "/products/37070",
		      "updated": "2011-03-24T04:34:27.251+0000",
		      "created": "2011-03-24T04:34:27.251+0000"
		    },
		    {
		      "name": "Large File Support Bits",
		      "id": "37068",
		      "attributes": [
		        {
		          "name": "version",
		          "value": "1.0",
		          "updated": "2011-03-24T04:34:30.292+0000",
		          "created": "2011-03-24T04:34:30.292+0000"
		        },
		        {
		          "name": "variant",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:30.293+0000",
		          "created": "2011-03-24T04:34:30.293+0000"
		        },
		        {
		          "name": "sockets",
		          "value": "2",
		          "updated": "2011-03-24T04:34:30.293+0000",
		          "created": "2011-03-24T04:34:30.293+0000"
		        },
		        {
		          "name": "arch",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:30.293+0000",
		          "created": "2011-03-24T04:34:30.293+0000"
		        },
		        {
		          "name": "type",
		          "value": "SVC",
		          "updated": "2011-03-24T04:34:30.293+0000",
		          "created": "2011-03-24T04:34:30.293+0000"
		        },
		        {
		          "name": "warning_period",
		          "value": "30",
		          "updated": "2011-03-24T04:34:30.293+0000",
		          "created": "2011-03-24T04:34:30.293+0000"
		        }
		      ],
		      "multiplier": 1,
		      "productContent": [
		        {
		          "content": {
		            "name": "always-enabled-content",
		            "id": "1",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/always",
		            "label": "always-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/always/gpg",
		            "metadataExpire": 200,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.415+0000",
		            "created": "2011-03-24T04:34:25.415+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": true
		        },
		        {
		          "content": {
		            "name": "never-enabled-content",
		            "id": "0",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/never",
		            "label": "never-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/never/gpg",
		            "metadataExpire": 600,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.277+0000",
		            "created": "2011-03-24T04:34:25.277+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": false
		        }
		      ],
		      "dependentProductIds": [

		      ],
		      "href": "/products/37068",
		      "updated": "2011-03-24T04:34:30.292+0000",
		      "created": "2011-03-24T04:34:30.292+0000"
		    },
		    {
		      "name": "Shared Storage Bits",
		      "id": "37067",
		      "attributes": [
		        {
		          "name": "version",
		          "value": "1.0",
		          "updated": "2011-03-24T04:34:28.860+0000",
		          "created": "2011-03-24T04:34:28.860+0000"
		        },
		        {
		          "name": "variant",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:28.861+0000",
		          "created": "2011-03-24T04:34:28.861+0000"
		        },
		        {
		          "name": "sockets",
		          "value": "2",
		          "updated": "2011-03-24T04:34:28.861+0000",
		          "created": "2011-03-24T04:34:28.861+0000"
		        },
		        {
		          "name": "arch",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:28.861+0000",
		          "created": "2011-03-24T04:34:28.861+0000"
		        },
		        {
		          "name": "type",
		          "value": "SVC",
		          "updated": "2011-03-24T04:34:28.861+0000",
		          "created": "2011-03-24T04:34:28.861+0000"
		        },
		        {
		          "name": "warning_period",
		          "value": "30",
		          "updated": "2011-03-24T04:34:28.861+0000",
		          "created": "2011-03-24T04:34:28.861+0000"
		        }
		      ],
		      "multiplier": 1,
		      "productContent": [
		        {
		          "content": {
		            "name": "always-enabled-content",
		            "id": "1",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/always",
		            "label": "always-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/always/gpg",
		            "metadataExpire": 200,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.415+0000",
		            "created": "2011-03-24T04:34:25.415+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": true
		        },
		        {
		          "content": {
		            "name": "never-enabled-content",
		            "id": "0",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/never",
		            "label": "never-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/never/gpg",
		            "metadataExpire": 600,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.277+0000",
		            "created": "2011-03-24T04:34:25.277+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": false
		        }
		      ],
		      "dependentProductIds": [

		      ],
		      "href": "/products/37067",
		      "updated": "2011-03-24T04:34:28.859+0000",
		      "created": "2011-03-24T04:34:28.859+0000"
		    },
		    {
		      "name": "Management Bits",
		      "id": "37069",
		      "attributes": [
		        {
		          "name": "version",
		          "value": "1.0",
		          "updated": "2011-03-24T04:34:31.181+0000",
		          "created": "2011-03-24T04:34:31.181+0000"
		        },
		        {
		          "name": "variant",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:31.181+0000",
		          "created": "2011-03-24T04:34:31.181+0000"
		        },
		        {
		          "name": "sockets",
		          "value": "2",
		          "updated": "2011-03-24T04:34:31.181+0000",
		          "created": "2011-03-24T04:34:31.181+0000"
		        },
		        {
		          "name": "arch",
		          "value": "ALL",
		          "updated": "2011-03-24T04:34:31.181+0000",
		          "created": "2011-03-24T04:34:31.181+0000"
		        },
		        {
		          "name": "type",
		          "value": "SVC",
		          "updated": "2011-03-24T04:34:31.181+0000",
		          "created": "2011-03-24T04:34:31.181+0000"
		        },
		        {
		          "name": "warning_period",
		          "value": "30",
		          "updated": "2011-03-24T04:34:31.181+0000",
		          "created": "2011-03-24T04:34:31.181+0000"
		        }
		      ],
		      "multiplier": 1,
		      "productContent": [
		        {
		          "content": {
		            "name": "always-enabled-content",
		            "id": "1",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/always",
		            "label": "always-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/always/gpg",
		            "metadataExpire": 200,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.415+0000",
		            "created": "2011-03-24T04:34:25.415+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": true
		        },
		        {
		          "content": {
		            "name": "never-enabled-content",
		            "id": "0",
		            "type": "yum",
		            "modifiedProductIds": [

		            ],
		            "contentUrl": "/foo/path/never",
		            "label": "never-enabled-content",
		            "vendor": "test-vendor",
		            "gpgUrl": "/foo/path/never/gpg",
		            "metadataExpire": 600,
		            "requiredTags": null,
		            "updated": "2011-03-24T04:34:25.277+0000",
		            "created": "2011-03-24T04:34:25.277+0000"
		          },
		          "flexEntitlement": 0,
		          "physicalEntitlement": 0,
		          "enabled": false
		        }
		      ],
		      "dependentProductIds": [

		      ],
		      "href": "/products/37069",
		      "updated": "2011-03-24T04:34:31.180+0000",
		      "created": "2011-03-24T04:34:31.180+0000"
		    }
		  ],
		  "endDate": "2013-03-13T00:00:00.000+0000",
		  "startDate": "2012-03-13T00:00:00.000+0000",
		  "quantity": 15,
		  "contractNumber": "20",
		  "accountNumber": "12331131231",
		  "modified": null,
		  "tokens": [

		  ],
		  "upstreamPoolId": null,
		  "updated": "2011-03-24T04:34:39.627+0000",
		  "created": "2011-03-24T04:34:39.627+0000"
		}
	*/
	
	
	@DataProvider(name="getAllJSONPoolsData")
	public Object[][] getAllJSONPoolsDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllJSONPoolsDataAsListOfLists());
	}
	/**
	 * @return List of [JSONObject jsonPool]
	 */
	protected List<List<Object>> getAllJSONPoolsDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// who is the owner of sm_clientUsername
		String clientOrg = sm_clientOrg;
		if (clientOrg==null) {
			List<RegistrationData> registrationData = findGoodRegistrationData(true,sm_clientUsername,false,clientOrg);
			if (registrationData.isEmpty() || registrationData.size()>1) throw new SkipException("Could not determine unique owner for username '"+sm_clientUsername+"'.  It is needed for a candlepin API call get pools by owner.");
			clientOrg = registrationData.get(0).ownerKey;
		}
		
		// process all of the pools belonging to ownerKey
		JSONArray jsonPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/owners/"+clientOrg+"/pools?listall=true"));	
		for (int i = 0; i < jsonPools.length(); i++) {
			JSONObject jsonPool = (JSONObject) jsonPools.get(i);
			
			// exclude sub pools that were generated from consumption of a parent pool
			// including these has cause tests to mysteriously fail because the pool can be deleted if the source entitlement is revoked which can happen if the current consumer who has generated this subpool unregisters before the test that uses this data provider is executed. 
			if (!jsonPool.isNull("sourceEntitlement")) continue;
			
			ll.add(Arrays.asList(new Object[]{jsonPool}));
		}
		
		return ll;
	}

	
	@DataProvider(name="getAllFutureJSONPoolsData")
	public Object[][] getAllFutureJSONPoolsDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllFutureJSONPoolsDataAsListOfLists(null));
	}
	@DataProvider(name="getAllFutureSystemJSONPoolsData")
	public Object[][] getAllFutureSystemJSONPoolsDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllFutureJSONPoolsDataAsListOfLists(ConsumerType.system));
	}
	@DataProvider(name="getAllFutureSystemSubscriptionPoolsData")
	public Object[][] getAllFutureSystemSubscriptionPoolsDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllFutureSystemSubscriptionPoolsDataAsListOfLists());
	}
	@DataProvider(name="getRandomSubsetOfFutureSystemSubscriptionPoolsData")
	public Object[][] getRandomSubsetOfFutureSystemSubscriptionPoolsDataAs2dArray() throws Exception {
		int subsetSize = 10;	// maximum subset count of data rows to return
		return TestNGUtils.convertListOfListsTo2dArray(getRandomSubsetOfList(getAllFutureSystemSubscriptionPoolsDataAsListOfLists(),subsetSize));
	}
	
	/**
	 * @return List of [SubscriptionPool pool]
	 */
	protected List<List<Object>>getAllFutureSystemSubscriptionPoolsDataAsListOfLists() throws ParseException, JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getAllFutureJSONPoolsDataAsListOfLists(ConsumerType.system)) {
			JSONObject jsonPool = (JSONObject) l.get(0);
/* DELETEME replaced by call to parseISO8601DateString
//			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
//			Calendar endDate = new GregorianCalendar();
//			endDate.setTimeInMillis(dateFormat.parse(jsonPool.getString("endDate")).getTime());
*/
			Calendar endDate = parseISO8601DateString(jsonPool.getString("endDate"), "GMT");
			
			Boolean multiEntitlement = CandlepinTasks.isPoolProductMultiEntitlement(sm_clientUsername,sm_clientPassword, sm_serverUrl, jsonPool.getString("id"));

			String quantity = String.valueOf(jsonPool.getInt("quantity"));
			
			ll.add(Arrays.asList(new Object[]{new SubscriptionPool(jsonPool.getString("productName"), jsonPool.getString("productId"), jsonPool.getString("id"), quantity, null, multiEntitlement, SubscriptionPool.formatDateString(endDate))}));
		}
		return ll;
	}
	
	/**
	 * @return List of [JSONObject jsonPool]
	 */
	protected List<List<Object>> getAllFutureJSONPoolsDataAsListOfLists(ConsumerType consumerType) throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// get the owner key for clientusername, clientpassword
		String consumerId = clienttasks.getCurrentConsumerId();
		if (consumerId==null) consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null));
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);

		for (List<Object> l : getAllFutureJSONSubscriptionsDataAsListOfLists(consumerType)) {
			JSONObject jsonSubscription = (JSONObject) l.get(0);
			String subscriptionId = jsonSubscription.getString("id");			
			Calendar startDate = parseISO8601DateString(jsonSubscription.getString("startDate"),"GMT");	// "startDate":"2012-02-08T00:00:00.000+0000"

			// process all of the pools belonging to ownerKey that are activeon startDate
			JSONArray jsonPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/owners/"+ownerKey+"/pools" +"?activeon="+urlEncode(formatISO8601DateString(startDate))));
			for (int j = 0; j < jsonPools.length(); j++) {
				JSONObject jsonPool = (JSONObject) jsonPools.get(j);
				
				// remember all the jsonPools that come from subscriptionId
				if (jsonPool.isNull("subscriptionId")) continue;	// will happen if there is a generated sub-pool from a consumed person subscription
				if (jsonPool.getString("subscriptionId").equals(subscriptionId)) {

					// JSONObject jsonPool
					ll.add(Arrays.asList(new Object[]{jsonPool}));
					
					// minimize the number of dataProvided rows (useful during automated testcase development)
					if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
				}
			}
		}
		return ll;
	}
	
	/**
	 * @return List of [JSONObject jsonSubscription]
	 */
	protected List<List<Object>> getAllFutureJSONSubscriptionsDataAsListOfLists(ConsumerType consumerType) throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// get the owner key for clientusername, clientpassword
		String consumerId = clienttasks.getCurrentConsumerId();
		if (consumerId==null) consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null));
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
		boolean isSystemVirtual = Boolean.valueOf(clienttasks.getFactValue("virt.is_guest"));
		
		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		
		// process all of the subscriptions belonging to ownerKey
		/* 7/10/2015 devel consciously decided to drop @Verify(value = Owner.class, subResource = SubResource.SUBSCRIPTIONS) on this GET method starting with candlepin-2.0.
		 * 7/10/2015 modifying this testware to simply raise the authentication credentials to admin
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));
		 */
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));
		for (int i = 0; i < jsonSubscriptions.length(); i++) {
			JSONObject jsonSubscription = (JSONObject) jsonSubscriptions.get(i);
			String id = jsonSubscription.getString("id");			
			Calendar startDate = parseISO8601DateString(jsonSubscription.getString("startDate"),"GMT");	// "startDate":"2012-02-08T00:00:00.000+0000"
			Calendar endDate = parseISO8601DateString(jsonSubscription.getString("endDate"),"GMT");	// "endDate":"2013-02-07T00:00:00.000+0000"

			// skip subscriptions to a product that doesn't satisfy the requested consumerType
			JSONObject jsonProduct = jsonSubscription.getJSONObject("product");
			JSONArray jsonProductAttributes = jsonProduct.getJSONArray("attributes");
			String requires_consumer_type = null;
			Boolean physical_only = null;
			for (int j = 0; j < jsonProductAttributes.length(); j++) {
				JSONObject jsonProductAttribute = (JSONObject) jsonProductAttributes.get(j);
				if (jsonProductAttribute.getString("name").equals("requires_consumer_type")) {
					requires_consumer_type = jsonProductAttribute.getString("value");
				}
				if (jsonProductAttribute.getString("name").equals("physical_only")) {
					physical_only = Boolean.valueOf(jsonProductAttribute.getString("value"));
				}
			}
			if (requires_consumer_type==null) requires_consumer_type = ConsumerType.system.toString();	// the absence of a "requires_consumer_type" implies requires_consumer_type is system
			
			// skip subscriptions that do not match the consumer type
			if (!ConsumerType.valueOf(requires_consumer_type).equals(consumerType)) continue;
			
			/* Changed my mind about this because the this method is called getAll which should not filter results by system type
			// skip subscriptions that do not match the machine type
			if (physical_only!=null && physical_only && isSystemVirtual) continue;
			*/
			
			// process subscriptions that are in the future
			if (startDate.after(now)) {
			
				// JSONObject jsonSubscription
				ll.add(Arrays.asList(new Object[]{jsonSubscription}));
			}
		}
		return ll;
	}

	
// DELETEME
//	/**
//	 * @return List of [SubscriptionPool modifierPool, String label, List<String> modifiedProductIds, String requiredTags, List<SubscriptionPool> providingPools]
//	 */
//	protected List<List<Object>> getModifierSubscriptionDataAsListOfLists() throws JSONException, Exception {
//		List<List<Object>> ll = new ArrayList<List<Object>>();	if (!isSetupBeforeSuiteComplete) return ll;
//		
//		// get the owner key for clientusername, clientpassword
//		String consumerId = clienttasks.getCurrentConsumerId();
//		if (consumerId==null) consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, Boolean.TRUE, false, null, null, null));
//		//String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, consumerId);
//
//		
//		List<SubscriptionPool> allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
//		
//		// iterate through all available pools looking for those that contain products with content that modify other products
//		for (SubscriptionPool modifierPool : allAvailablePools) {
//			JSONObject jsonModifierPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+modifierPool.poolId));	
//			
//			// iterate through each of the providedProducts
//			JSONArray jsonModifierProvidedProducts = jsonModifierPool.getJSONArray("providedProducts");
//			for (int i = 0; i < jsonModifierProvidedProducts.length(); i++) {
//				JSONObject jsonModifierProvidedProduct = (JSONObject) jsonModifierProvidedProducts.get(i);
//				String modifierProvidedProductId = jsonModifierProvidedProduct.getString("productId");
//				
//				// get the productContents
//				JSONObject jsonProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/products/"+modifierProvidedProductId));	
//				JSONArray jsonProductContents = jsonProduct.getJSONArray("productContent");
//				for (int j = 0; j < jsonProductContents.length(); j++) {
//					JSONObject jsonProductContent = (JSONObject) jsonProductContents.get(j);
//					JSONObject jsonContent = jsonProductContent.getJSONObject("content");
//					
//					// get the label and modifiedProductIds for each of the productContents
//					String label = jsonContent.getString("label");
//					String requiredTags = jsonContent.getString("requiredTags"); // comma separated string
//					if (requiredTags.equals("null")) requiredTags = null;
//					JSONArray jsonModifiedProductIds = jsonContent.getJSONArray("modifiedProductIds");
//					List<String> modifiedProductIds = new ArrayList<String>();
//					for (int k = 0; k < jsonModifiedProductIds.length(); k++) {
//						String modifiedProductId = (String) jsonModifiedProductIds.get(k);
//						modifiedProductIds.add(modifiedProductId);
//					}
//					
//					// does this pool contain productContents that modify other products?
//					if (modifiedProductIds.size()>0) {
//						
//						List<SubscriptionPool> providingPools = new ArrayList<SubscriptionPool>();
//						// yes, now its time to find the subscriptions that provide the modifiedProductIds
//						for (SubscriptionPool providingPool : allAvailablePools) {
//							JSONObject jsonProvidingPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+providingPool.poolId));	
//							
//							// iterate through each of the providedProducts
//							JSONArray jsonProvidingProvidedProducts = jsonProvidingPool.getJSONArray("providedProducts");
//							for (int l = 0; l < jsonProvidingProvidedProducts.length(); l++) {
//								JSONObject jsonProvidingProvidedProduct = (JSONObject) jsonProvidingProvidedProducts.get(l);
//								String providingProvidedProductId = jsonProvidingProvidedProduct.getString("productId");
//								if (modifiedProductIds.contains(providingProvidedProductId)) {
//									
//									// NOTE: This test takes a long time to run when there are many providingPools.
//									// To reduce the execution time, let's simply limit the number of providing pools tested to 2,
//									// otherwise this block of code could be commented out for a more thorough test.
//									boolean thisPoolProductIdIsAlreadyInProvidingPools = false;
//									for (SubscriptionPool providedPool : providingPools) {
//										if (providedPool.productId.equals(providingPool.productId)) {
//											thisPoolProductIdIsAlreadyInProvidingPools=true; break;
//										}
//									}
//									if (thisPoolProductIdIsAlreadyInProvidingPools||providingPools.size()>=2) break;
//									
//									providingPools.add(providingPool); break; // FIXME I THINK THE BREAK IS A TYPO SHOULD BE REMOVED
//								}
//							}
//						}
//										
//						ll.add(Arrays.asList(new Object[]{modifierPool, label, modifiedProductIds, requiredTags, providingPools}));
//					}
//				}
//			}
//		}	
//		return ll;
//	}
	/**
	 * @param limitPoolsModifiedCount This test takes a long time to run when there are many poolsModified.
	 * To reduce the execution time, use limitPoolsModifiedCount to limit the number of modifies pools tested,
	 * otherwise pass null to to include all poolsModified in the data for a more complete test. Update 4/2/2015: I think this is a bad idea because it could potentially hide the case where the modifierPool actually modifies itself (like the Employee SKU or RH00003).  For now always pass null.)
	 * @return List of [SubscriptionPool modifierPool, String label, List<String> modifiedProductIds, String requiredTags, List<SubscriptionPool> poolsModified]
	 */
	protected List<List<Object>> getModifierSubscriptionDataAsListOfLists(Integer limitPoolsModifiedCount) throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();	if (!isSetupBeforeSuiteComplete) return ll;
		JSONObject jsonStatus = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(/*authenticator*/null,/*password*/null,sm_serverUrl,"/status"));
		List<SubscriptionPool> allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		
		// iterate through all available pools looking for those that contain products with content that modify other products
		// use maps to reduce repeated Candlepin calls and save a lot of network time
		Map <String,JSONObject> jsonPoolMap = new HashMap<String,JSONObject>();	// keys are poolId, values are jsonPool
		Map <String,JSONObject> jsonProductMap = new HashMap<String,JSONObject>();	// keys are productId, values are jsonProduct
		for (SubscriptionPool modifierPool : allAvailablePools) {
			//JSONObject jsonModifierPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+modifierPool.poolId));	
			if (!jsonPoolMap.containsKey(modifierPool.poolId)) jsonPoolMap.put(modifierPool.poolId, new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+modifierPool.poolId+"?include=providedProducts.productId&include=owner.href")));
			JSONObject jsonModifierPool = jsonPoolMap.get(modifierPool.poolId);
			JSONArray jsonModifierProvidedProducts = jsonModifierPool.getJSONArray("providedProducts");
			
			// collect all of the provided product ids for this modifier pool
			List<String> poolModifierProvidedProductIds = new ArrayList<String>();
			for (int l = 0; l < jsonModifierProvidedProducts.length(); l++) {
				JSONObject jsonModifierProvidedProduct = (JSONObject) jsonModifierProvidedProducts.get(l);
				poolModifierProvidedProductIds.add(jsonModifierProvidedProduct.getString("productId"));
			}
			
			// iterate through each of the providedProducts
			MODIFIER_LOOP_FOR_ITS_PROVIDED_PRODUCTS : for (int i = 0; i < jsonModifierProvidedProducts.length(); i++) {
				JSONObject jsonModifierProvidedProduct = (JSONObject) jsonModifierProvidedProducts.get(i);
				String modifierProvidedProductId = jsonModifierProvidedProduct.getString("productId");
				
				// get the productContents
				String path = "/products/"+modifierProvidedProductId;
				if (SubscriptionManagerTasks.isVersion(jsonStatus.getString("version"),">=","2.0.11")) path = jsonModifierPool.getJSONObject("owner").getString("href")+path;	// starting with candlepin-2.0.11 /products/<ID> are requested by /owners/<KEY>/products/<ID> OR /products/<UUID>
				if (!jsonProductMap.containsKey(modifierProvidedProductId)) jsonProductMap.put(modifierProvidedProductId, new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,path+"?include=productContent.content")));
				JSONObject jsonProduct = jsonProductMap.get(modifierProvidedProductId);
				JSONArray jsonProductContents = jsonProduct.getJSONArray("productContent");
				for (int j = 0; j < jsonProductContents.length(); j++) {
					JSONObject jsonProductContent = (JSONObject) jsonProductContents.get(j);
					JSONObject jsonContent = jsonProductContent.getJSONObject("content");
					
					// get the label and modifiedProductIds for each of the productContents
					String label = jsonContent.getString("label");
					String requiredTags = jsonContent.isNull("requiredTags")? null:jsonContent.getString("requiredTags"); // comma separated string
					String type = jsonContent.getString("type");
					JSONArray jsonModifiedProductIds = jsonContent.getJSONArray("modifiedProductIds");
					List<String> modifiedProductIds = new ArrayList<String>();
					for (int k = 0; k < jsonModifiedProductIds.length(); k++) {
						String modifiedProductId = (String) jsonModifiedProductIds.get(k);
						modifiedProductIds.add(modifiedProductId);
					}
					
					// skip non-"yum" content like this example which does not make sense to me...  why would a content set that contains isos modify a product?
					//	[root@jsefler-os6 ~]# curl --stderr /dev/null --insecure --user stage_auto_testuser:redhat --request GET https://subscription.rhn.stage.redhat.com/subscription/products/70?include=productContent.content | python -m simplejson/tool | grep "rhel-6-server-eus-supplementary-isos" -A10 -B5
					//        "content": {
					//            "arches": "x86,x86_64", 
					//            "contentUrl": "/content/eus/rhel/server/6/$releasever/$basearch/supplementary/iso", 
					//            "gpgUrl": "http://", 
					//            "id": "685", 
					//            "label": "rhel-6-server-eus-supplementary-isos", 
					//            "metadataExpire": 86400, 
					//            "modifiedProductIds": [
					//                "69"
					//            ], 
					//            "name": "Red Hat Enterprise Linux 6 Server - Extended Update Support - Supplementary (ISOs)", 
					//            "releaseVer": null, 
					//            "requiredTags": "rhel-6-server", 
					//            "type": "file", 
					//            "vendor": "Red Hat"
					//        }
					if (!type.equalsIgnoreCase("yum")) continue;
					
					
					// does this pool contain productContents that modify other products?
					if (!modifiedProductIds.isEmpty()) {
						
						// yes it does...  this is truly a modifier pool!
						
						// avoid a circular situation, exclude/skip modifier pools that provide a modifier product that modifies a product that is already provided by this pool						
						boolean doesThisPoolModifyItself = false;
						for (String modifiedProductId : modifiedProductIds) {
							if (poolModifierProvidedProductIds.contains(modifiedProductId)) {
								doesThisPoolModifyItself = true;
								log.warning("Encountered this circular pool which appears to provide a modifier product id='"+modifierProvidedProductId+"' containing content label '"+label+"' that modifies product ids "+modifiedProductIds+" for which at least one is already among the provided products "+poolModifierProvidedProductIds+" of this pool: "+modifierPool);
								log.warning("Excluding this pool since it appears to modify itself.  The product modifier and product modified are both among the provided products "+modifierPool.provides);
								break MODIFIER_LOOP_FOR_ITS_PROVIDED_PRODUCTS;
							}
						}
						
						// now its time to find the pools that provide the modifiedProductIds and therefore are considered to be the pools that provide products that are modified
						List<SubscriptionPool> poolsModified = new ArrayList<SubscriptionPool>();
						MODIFIED_LOOP_FOR_ALL_AVAILABLE_POOLS: for (SubscriptionPool poolModified : allAvailablePools) {
							
							//JSONObject jsonPoolModified = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+poolModified.poolId));	
							if (!jsonPoolMap.containsKey(poolModified.poolId)) jsonPoolMap.put(poolModified.poolId, new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+poolModified.poolId+"?include=providedProducts.productId&include=owner.href")));
							JSONObject jsonPoolModified = jsonPoolMap.get(poolModified.poolId);
							
							// iterate through each of the providedProducts of the poolModified
							JSONArray jsonPoolModifiedProvidedProducts = jsonPoolModified.getJSONArray("providedProducts");
							
							// collect all of the provided product ids for this modified pool
							List<String> poolModifiedProvidedProductIds = new ArrayList<String>();
							for (int l = 0; l < jsonPoolModifiedProvidedProducts.length(); l++) {
								JSONObject jsonPoolModifiedProvidedProduct = (JSONObject) jsonPoolModifiedProvidedProducts.get(l);
								poolModifiedProvidedProductIds.add(jsonPoolModifiedProvidedProduct.getString("productId"));
							}
							
							// to avoid a circular situation, exclude/skip pools that provide the same modifier product from poolsModified				
							if (poolModifiedProvidedProductIds.contains(modifierProvidedProductId)) {
								log.warning("Excluding modified subscription '"+poolModified.subscriptionName+"' which provides "+poolModified.provides+" from testing since it appears to provide the same modifier product '"+modifierProvidedProductId+"' that we are testing from modifier pool: "+modifierPool);
								continue;
							}
							
							for (int l = 0; l < jsonPoolModifiedProvidedProducts.length(); l++) {
								JSONObject jsonPoolModifiedProvidedProduct = (JSONObject) jsonPoolModifiedProvidedProducts.get(l);
								if (modifiedProductIds.contains(jsonPoolModifiedProvidedProduct.getString("productId"))) {
									
// DELETEME - BAD IDEA SINCE IT GIVES THE ILLUSION THAT ANOTHER POOL INSTANCE ON THE SAME SUBSCRIPTION (second contract number) APPEARS AS THOUGH IT DOES NOT QUALIFY AS A MODIFIED POOL
//									// no need to add this poolModified if the product it represents was already added to the poolsModified
//									boolean thisPoolModifiedProductIdIsAlreadyInPoolsModified = false;
//									for (SubscriptionPool pool : poolsModified) {
//										if (pool.productId.equals(poolModified.productId)) {
//											thisPoolModifiedProductIdIsAlreadyInPoolsModified=true; break;
//										}
//									}
//									if (thisPoolModifiedProductIdIsAlreadyInPoolsModified) break;
									
									poolsModified.add(poolModified);
									
									if (limitPoolsModifiedCount!=null && poolsModified.size()>=limitPoolsModifiedCount) break MODIFIED_LOOP_FOR_ALL_AVAILABLE_POOLS;
								}
							}
						}
										
						ll.add(Arrays.asList(new Object[]{modifierPool, label, modifiedProductIds, requiredTags, poolsModified}));
					}
				}
			}
		}	
		return ll;
	}

	
	@DataProvider(name="getRandomSubsetOfAllAvailableServiceLevelData")
	public Object[][] getRandomSubsetOfAllAvailableServiceLevelDataAs2dArray() throws JSONException, Exception {
		int subsetSize = 3;
		return TestNGUtils.convertListOfListsTo2dArray(getRandomSubsetOfList(getAllAvailableServiceLevelDataAsListOfLists(),subsetSize));
	}
	@DataProvider(name="getAllAvailableServiceLevelData")
	public Object[][] getAllAvailableServiceLevelDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllAvailableServiceLevelDataAsListOfLists());
	}
	/**
	 * @return List of [Object bugzilla, String serviceLevel]
	 */
	protected List<List<Object>>getAllAvailableServiceLevelDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register with force (so we can find the org to which the sm_clientUsername belongs in case sm_clientOrg is null)
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "SubscriptionServiceLevelConsumer", null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null));
		String org = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername,sm_clientPassword,sm_serverUrl,consumerId);
		
		// get all the valid service levels available to this org
		// Object bugzilla, String serviceLevel
 		if (sm_serverOld) {
	 		for (String serviceLevel : clienttasks.getCurrentlyAvailableServiceLevels()) {
	 			ll.add(Arrays.asList(new Object[] {null,	serviceLevel}));
	 		}
 		} else
		for (String serviceLevel : CandlepinTasks.getServiceLevelsForOrgKey(sm_clientUsername, sm_clientPassword, sm_serverUrl, org)) {
			ll.add(Arrays.asList(new Object[] {null,	serviceLevel}));
		}
				
		return ll;
	}
	
	
	
	
	@DataProvider(name="getServerurlData")
	public Object[][] getServerurlDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getServerurlDataAsListOfLists());
	}
	protected List<List<Object>> getServerurlDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (servertasks==null) return ll;
		if (clienttasks==null) return ll;
		String defaultHostname = "subscription.rhn.redhat.com";
		String defaultPort = "443";
		String defaultPrefix = "/subscription";
		String serverurl;
		
		// initialize server_hostname server_port server_prefix with valid values from the current rhsm configurations
		String server_hostname	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		String server_port		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
		String server_prefix	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
		
		//  --serverurl=SERVER_URL      server url in the form of https://hostname:443/prefix

		// Object bugzilla, String serverurl, String expectedHostname, String expectedPort, String expectedPrefix, Integer expectedExitCode, String expectedStdout, String expectedStderr
		// positive tests
		serverurl= server_hostname+(server_port.isEmpty()?"":":"+server_port)+server_prefix;			ll.add(Arrays.asList(new Object[] {	null,	serverurl,	server_hostname,	server_port,	server_prefix,		new Integer(0),	null,	null}));
		serverurl= "https://"+serverurl;																ll.add(Arrays.asList(new Object[] {	null,	serverurl,	server_hostname,	server_port,	server_prefix,		new Integer(0),	null,	null}));
		
		if (server_port.equals(defaultPort)) {
			serverurl= server_hostname+server_prefix;													ll.add(Arrays.asList(new Object[] {	null,	serverurl,	server_hostname,	defaultPort,	server_prefix,		new Integer(0),	null,	null}));
			serverurl= "https://"+serverurl;															ll.add(Arrays.asList(new Object[] {	null,	serverurl,	server_hostname,	defaultPort,	server_prefix,		new Integer(0),	null,	null}));
		}
		if (server_prefix.equals(defaultPrefix)) {
			serverurl= server_hostname+(server_port.isEmpty()?"":":"+server_port);						ll.add(Arrays.asList(new Object[] {	null,	serverurl,	server_hostname,	server_port,	defaultPrefix,		new Integer(0),	null,	null}));
			serverurl= "https://"+serverurl;															ll.add(Arrays.asList(new Object[] {	null,	serverurl,	server_hostname,	server_port,	defaultPrefix,		new Integer(0),	null,	null}));
		}
		if (server_hostname.equals(defaultHostname)) {
			serverurl= (server_port.isEmpty()?"":":"+server_port);										ll.add(Arrays.asList(new Object[] {	null,	serverurl,	defaultHostname,	server_port,	server_prefix,		new Integer(0),	null,	null}));
			serverurl= "https://"+serverurl;															ll.add(Arrays.asList(new Object[] {	null,	serverurl,	defaultHostname,	server_port,	server_prefix,		new Integer(0),	null,	null}));
		}
		// TODO add a case for the ipaddress of hostname
		
		// ignored tests
		serverurl="";																					ll.add(Arrays.asList(new Object[] {	null,	serverurl,	/* last set */ ll.get(ll.size()-1).get(2),	ll.get(ll.size()-1).get(3),	ll.get(ll.size()-1).get(4),	new Integer(0),	null,	null}));	
	
		// negative tests
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.17.5-1")) {	// post commit ea10b99095ad58df57ed107e13bf19498e003ae8	// Bug 1320507 - Wrong prefix prompts when register using serverurl without prefix
//			if (isCurrentlyConfiguredServerTypeHosted()) {
			if (isCurrentlyConfiguredServerTypeHosted() && SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "0.9.51.21-1")) {
				// 08-11-2015, I don't like this behavior because IT is blacklisting any prefix that does not match /subscription causing a inaccessible server to masquerade as a CA certificate error"
				// 02-17-2017, Good News - yesterday IT bumped candlepin-0.9.51.20-1 to 0.9.51.21-1 and the CA certificate error response disappeared.
				serverurl= "https://"+server_hostname+(server_port.isEmpty()?"":":"+server_port)+"/PREFIX";		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","842885"}),								serverurl,	null,	null,	null,		new Integer(78),	null,						"Error: CA certificate for subscription service has not been installed."}));
				serverurl= "/";																					ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","830767"}),								serverurl,	null,	null,	null,		new Integer(78),	null,						"Error: CA certificate for subscription service has not been installed."}));
			} else {
				serverurl= "https://"+server_hostname+(server_port.isEmpty()?"":":"+server_port)+"/PREFIX";		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","842885"}),								serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at "+server_hostname+(server_port.isEmpty()?":"+defaultPort:":"+server_port)+"/PREFIX"}));
				serverurl= "/";																					ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1320507","830767","1176219"}),			serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at "+server_hostname+":"+server_port+"/?"}));	// the ending ? means that we'll accept this expected stderr message with and without the trailing slash / and was added post commit ad982c13e79917e082f336255ecc42615e1e7707 Bug 1176219 - subscription-manager repos --list with bad proxy options is silently using cache
			}
			serverurl= "hostname";																			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1320507"}),									serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at hostname:"+server_port+server_prefix}));
			serverurl= "hostname:900";																		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1320507"}),									serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at hostname:900"+server_prefix}));
			serverurl= "hostname:900/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688"}),											serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at hostname:900/prefix"}));
			serverurl= "https://"+server_hostname+":PORT"+server_prefix;									ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634","842845"}),		serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL port should be numeric"}));
			serverurl= "https://hostname:PORT/prefix";														ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634","842845"}),		serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL port should be numeric"}));
			serverurl= "https://hostname:/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634"}),				serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL port should be numeric"}));
			serverurl= "https:/hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "https:hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "https//hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "https/hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "ftp://hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "git://hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "https://";																			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL is just a schema. Should include hostname, and/or port and path"}));
			serverurl= "http://";																			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL is just a schema. Should include hostname, and/or port and path"}));
			//TODO serverurl= "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py";			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL can not be empty"}));
			//TODO serverurl= "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py";			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL can not be None"}));

		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			if (isCurrentlyConfiguredServerTypeHosted()) {
				// 08-11-2015, I don't like this behavior because IT is blacklisting any prefix that does not match /subscription causing a inaccessible server to masquarade as a CA certificate error"
				serverurl= "https://"+server_hostname+(server_port.isEmpty()?"":":"+server_port)+"/PREFIX";		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","842885"}),								serverurl,	null,	null,	null,		new Integer(78),	null,						"Error: CA certificate for subscription service has not been installed."}));
			} else {
				serverurl= "https://"+server_hostname+(server_port.isEmpty()?"":":"+server_port)+"/PREFIX";		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","842885"}),								serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at "+server_hostname+(server_port.isEmpty()?":"+defaultPort:":"+server_port)+"/PREFIX"}));
			}
			serverurl= "hostname";																			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688"}),											serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at hostname:"+defaultPort+defaultPrefix}));
			serverurl= "hostname:900";																		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688"}),											serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at hostname:900"+defaultPrefix}));
			serverurl= "hostname:900/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688"}),											serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at hostname:900/prefix"}));
			// 08-11-2015, I don't like this new behavior because IT is blacklisting any prefix that does not match /subscription causing a inaccessible server to masquarade as a CA certificate error" because sub-man default to subscription.rhn.redhat.com when passing / as the serverurl
			//serverurl= "/";																					ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","830767"}),								serverurl,	null,	null,	null,		new Integer(69),	null,						"Unable to reach the server at "+defaultHostname+":"+defaultPort+"/"}));
			serverurl= "/";																					ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","830767"}),									serverurl,	null,	null,	null,		new Integer(78),	null,						"Error: CA certificate for subscription service has not been installed."}));
			serverurl= "https://"+server_hostname+":PORT"+server_prefix;									ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634","842845"}),		serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL port should be numeric"}));
			serverurl= "https://hostname:PORT/prefix";														ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634","842845"}),		serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL port should be numeric"}));
			serverurl= "https://hostname:/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634"}),				serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL port should be numeric"}));
			serverurl= "https:/hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "https:hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "https//hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "https/hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "ftp://hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "git://hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			serverurl= "https://";																			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL is just a schema. Should include hostname, and/or port and path"}));
			serverurl= "http://";																			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL is just a schema. Should include hostname, and/or port and path"}));
			//TODO serverurl= "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py";			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL can not be empty"}));
			//TODO serverurl= "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py";			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:",	"Server URL can not be None"}));

		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			serverurl= "https://"+server_hostname+(server_port.isEmpty()?"":":"+server_port)+"/PREFIX";		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","842885"}),									serverurl,	null,	null,	null,		new Integer(69),	null,"Unable to reach the server at "+server_hostname+(server_port.isEmpty()?":"+defaultPort:":"+server_port)+"/PREFIX"}));
			serverurl= "hostname";																			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688"}),											serverurl,	null,	null,	null,		new Integer(69),	null,"Unable to reach the server at hostname:"+defaultPort+defaultPrefix}));
			serverurl= "hostname:900";																		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688"}),											serverurl,	null,	null,	null,		new Integer(69),	null,"Unable to reach the server at hostname:900"+defaultPrefix}));
			serverurl= "hostname:900/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688"}),											serverurl,	null,	null,	null,		new Integer(69),	null,"Unable to reach the server at hostname:900/prefix"}));
			serverurl= "/";																					ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","830767"}),									serverurl,	null,	null,	null,		new Integer(69),	null,"Unable to reach the server at "+defaultHostname+":"+defaultPort+"/"}));
			serverurl= "https://"+server_hostname+":PORT"+server_prefix;									ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634","842845"}),		serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL port should be numeric",null}));
			serverurl= "https://hostname:PORT/prefix";														ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634","842845"}),		serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL port should be numeric",null}));
			serverurl= "https://hostname:/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634"}),				serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL port should be numeric",null}));
			serverurl= "https:/hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",null}));
			serverurl= "https:hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",null}));
			serverurl= "https//hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",null}));
			serverurl= "https/hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",null}));
			serverurl= "ftp://hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",null}));
			serverurl= "git://hostname/prefix";																ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",null}));
			serverurl= "https://";																			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL is just a schema. Should include hostname, and/or port and path",null}));
			serverurl= "http://";																			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL is just a schema. Should include hostname, and/or port and path",null}));
			//TODO serverurl= "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py";			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL can not be empty",null}));
			//TODO serverurl= "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py";			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(70),	"Error parsing serverurl:\nServer URL can not be None",null}));

		} else {
	//		serverurl= "https://"+server_hostname+":PORT"+server_prefix;									ll.add(Arrays.asList(new Object[] {	null,															serverurl,	null,	null,	null,		new Integer(255),	"Unable to reach the server at "+server_hostname+":PORT"+server_prefix,													null}));
			serverurl= "https://"+server_hostname+(server_port.isEmpty()?"":":"+server_port)+"/PREFIX";		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug("842885"),									serverurl,	null,	null,	null,		new Integer(255),	"Unable to reach the server at "+server_hostname+(server_port.isEmpty()?":"+defaultPort:":"+server_port)+"/PREFIX",		null}));
			serverurl= "hostname";																			ll.add(Arrays.asList(new Object[] {	null,															serverurl,	null,	null,	null,		new Integer(255),	"Unable to reach the server at hostname:"+defaultPort+defaultPrefix,													null}));
			serverurl= "hostname:900";																		ll.add(Arrays.asList(new Object[] {	null,															serverurl,	null,	null,	null,		new Integer(255),	"Unable to reach the server at hostname:900"+defaultPrefix,																null}));
			serverurl= "hostname:900/prefix";																ll.add(Arrays.asList(new Object[] {	null,															serverurl,	null,	null,	null,		new Integer(255),	"Unable to reach the server at hostname:900/prefix",																	null}));
			serverurl= "/";																					ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug("830767"),									serverurl,	null,	null,	null,		new Integer(255),	"Unable to reach the server at "+defaultHostname+":"+defaultPort+"/",													null}));
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.12-1")) {
				serverurl= "https://"+server_hostname+":PORT"+server_prefix;								ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496","878634","842845"}),	serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL port should be numeric",												null}));
				serverurl= "https://hostname:PORT/prefix";													ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496","878634","842845"}),	serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL port should be numeric",												null}));
				serverurl= "https://hostname:/prefix";														ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496","878634"}),				serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL port should be numeric",												null}));
				serverurl= "https:/hostname/prefix";														ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
				serverurl= "https:hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
				serverurl= "https//hostname/prefix";														ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
				serverurl= "https/hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
				serverurl= "ftp://hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
				serverurl= "git://hostname/prefix";															ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
				serverurl= "https://";																		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL is just a schema. Should include hostname, and/or port and path",		null}));
				serverurl= "http://";																		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL is just a schema. Should include hostname, and/or port and path",		null}));
				//TODO serverurl= "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py";		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL can not be empty",	null}));
				//TODO serverurl= "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py";		ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl:\nServer URL can not be None",		null}));
			} else {
				serverurl= "https://"+server_hostname+":PORT"+server_prefix;								ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"878634","842845"}),	serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL port should be numeric",					null}));
				serverurl= "https://hostname:PORT/prefix";													ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"878634","842845"}),	serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL port should be numeric",					null}));
				serverurl= "https://hostname:/prefix";														ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"878634"}),				serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL port could not be parsed",					null}));
				serverurl= "https:/hostname/prefix";														ll.add(Arrays.asList(new Object[] {	null,													serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL has an invalid scheme. http:// and https:// are supported",					null}));
				serverurl= "https:hostname/prefix";															ll.add(Arrays.asList(new Object[] {	null,													serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL has an invalid scheme. http:// and https:// are supported",					null}));
				serverurl= "https//hostname/prefix";														ll.add(Arrays.asList(new Object[] {	null,													serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL has an invalid scheme. http:// and https:// are supported",					null}));
				serverurl= "https/hostname/prefix";															ll.add(Arrays.asList(new Object[] {	null,													serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL has an invalid scheme. http:// and https:// are supported",					null}));
				serverurl= "ftp://hostname/prefix";															ll.add(Arrays.asList(new Object[] {	null,													serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL has an invalid scheme. http:// and https:// are supported",					null}));
				serverurl= "git://hostname/prefix";															ll.add(Arrays.asList(new Object[] {	null,													serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL has an invalid scheme. http:// and https:// are supported",					null}));
				serverurl= "https://";																		ll.add(Arrays.asList(new Object[] {	null,													serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL is just a schema. Should include hostname, and/or port and path",					null}));
				serverurl= "http://";																		ll.add(Arrays.asList(new Object[] {	null,													serverurl,	null,	null,	null,		new Integer(255),	"Error parsing serverurl: Server URL is just a schema. Should include hostname, and/or port and path",					null}));
			}
		}
		
		return ll;
	}
	
	
	/**
	 * Write a big Set of Strings to a local file - Use a BufferedWriter
	 * @param lines
	 * @param file
	 * @param separator TODO
	 */
	protected void writeSetOfStringsToFile(Set<String> lines, File file, String separator) {
		Writer fileWriter = null;
		BufferedWriter bufferedWriter = null;
		try {
			fileWriter = new FileWriter(file);
			bufferedWriter = new BufferedWriter(fileWriter);
	
			// Write the lines one by one
			for (String line : lines) {
				bufferedWriter.write(line);
				if (separator==null) {
					bufferedWriter.newLine();
				} else {
					bufferedWriter.write(separator);
				}
			}
	
		} catch (IOException e) {
			System.err.println("Error writing the file : ");
			e.printStackTrace();
		} finally {
	
			if (bufferedWriter != null && fileWriter != null) {
				try {
					bufferedWriter.close();
					fileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}




	protected boolean isCurrentlyConfiguredServerTypeHosted() {
		return isHostnameHosted(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"));
	}




	protected boolean isHostnameHosted(String hostname) {
//		return hostname.matches("subscription\\.rhn\\.(.*\\.)*redhat\\.com");
		return hostname.matches("subscription\\.(rhn|rhsm)\\.(.*\\.)*redhat\\.com");
	}
	
	protected void setupRhnCACert() {
		// is rhn-client-tools package installed?
		boolean isRhnClientToolsInstalled = clienttasks.isPackageInstalled("rhn-client-tools");	// provides /etc/sysconfig/rhn/up2date and /usr/sbin/rhnreg_ks
		
		// make dir /etc/sysconfig/rhn/ when rhn-client-tools package is not installed to enable some more tesing
		if (!isRhnClientToolsInstalled) {
			String rhnDir =  new File(clienttasks.rhnSystemIdFile).getParent();
			client.runCommandAndWait("mkdir -p "+rhnDir);
		}
		
		
		// make sure we have the RHN-ORG-TRUSTED-SSL-CERT for the rhn/satellite server
		/*
		 * 	1. Set automation parameters:
		 * 		sm.rhn.hostname : https://sat-56-server.usersys.redhat.com
		 *		sm.rhn.username : admin
		 *		sm.rhn.password : *****
		 *  2. Use firefox to login to the Satellite account
		 *      https://sat-56-server.usersys.redhat.com/rhn/Login.do
		 *      do whatever work you need to there
		 *  3. Get the CA cert from Satellite and install it onto your client
		 *      wget --no-verbose --no-check-certificate --output-document=/usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT_sat-56-server.usersys.redhat.com https://sat-56-server.usersys.redhat.com/pub/RHN-ORG-TRUSTED-SSL-CERT
		 *  4. Update the /etc/sysconfig/rhn/up2date with
		 *      sslCACert=RHN-ORG-TRUSTED-SSL-CERT_sat-56-server.usersys.redhat.com
		 */
		// get the CA cert from Satellite and install it onto your client
		if (!sm_rhnHostname.isEmpty()) {
			if (!doesStringContainMatches(sm_rhnHostname, "rhn\\.(.+\\.)*redhat\\.com")) {	// if (sm_rhnHostname.startsWith("http") { 	// indicates that we are migrating from a non-hosted rhn server - as opposed to rhn.code.stage.redhat.com (stage) or rhn.redhat.com (production)
				String satHostname = sm_rhnHostname.split("/")[2];	// https://sat-56-server.usersys.redhat.com
				String satCaCertPath = "/usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT"+"_"+satHostname;
				RemoteFileTasks.runCommandAndAssert(client,"wget --no-verbose --no-check-certificate --output-document="+satCaCertPath+" "+sm_rhnHostname+"/pub/RHN-ORG-TRUSTED-SSL-CERT",Integer.valueOf(0),null,"-> \""+satCaCertPath+"\"");
				
				// update /etc/sysconfig/rhn/up2date->sslCACert with satCaCertPath
				clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "sslCACert", satCaCertPath);	// sslCACert[comment]=The CA cert used to verify the ssl server
			}
		}
		
		// make sure the rhnplugin conf is enabled
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForRhn, "enabled","1");
	}


	@AfterGroups(
			value = {"testSubscriptionManagerShouldAutomaticallyEnableYumPluginsWhenAutoEnableIsOn",
					"testSubscriptionManagerShouldNotAutomaticallyEnableYumPluginsWhenAutoEnableIsOff",
					"testRhnMigrateClassicToRhsmShouldAutomaticallyEnableYumPluginsWhenAutoEnableIsOn",
					"testRhnMigrateClassicToRhsmShouldAutomaticallyEnableYumPluginsWhenAutoEnableIsOff",
					"testSubscriptionManagerConfigModuleShouldNotAutomaticallyEnableYumPlugins"},
			groups = {"setup"})
	public void resetDefaultConfigurationsForYumPluginsAndRhsmAutoEnableYumPlugins() {
		// make sure subscription-manager config auto_enable_yum_plugins is on
		clienttasks.config(false,false,true,new String[]{"rhsm","auto_enable_yum_plugins","1"});
		
		// make sure yum plugins are enabled
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "1");
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "1");
	}


	protected List<ProductCert> productCertsBeforeThisTest;
	@BeforeGroups(groups = { "setup" }, value = {
			// list of individual tests that could cause the product-id yum plugin to install a productid from the CDN repodata that you want to remove after the test 
			"testWithNotifyOnlyOffVerifyYumSearchDisabledReposAssumingYesResponses",
			"testWithNotifyOnlyOffVerifyYumSearchDisabledReposWithYesYesNoResponses",
			"testInstallAndRemoveAnyPackageFromEnabledRepoAfterSubscribingToPool",
			"testInstallAndRemoveYumGroupFromEnabledRepoAfterSubscribingToPool",
			"testYumInstallSucceedsWhenServiceRsyslogIsStopped" })
	public void rememberProductCertsBeforeThisTest() {
		if (clienttasks==null) return;
		productCertsBeforeThisTest = clienttasks.getCurrentProductCerts();
	}
	
	@AfterGroups(groups={"setup"}, value={
			// list of individual tests that could cause the product-id yum plugin to install a productid from the CDN repodata that you want to remove after the test 
			"testWithNotifyOnlyOffVerifyYumSearchDisabledReposAssumingYesResponses",
			"testWithNotifyOnlyOffVerifyYumSearchDisabledReposWithYesYesNoResponses",
			"testInstallAndRemoveAnyPackageFromEnabledRepoAfterSubscribingToPool",
			"testInstallAndRemoveYumGroupFromEnabledRepoAfterSubscribingToPool",
			"testYumInstallSucceedsWhenServiceRsyslogIsStopped"},
			alwaysRun=true)
	public void restoreProductCertsAfterThisTest() {
		if (clienttasks==null) return;
		List<ProductCert> productCertsAfterThisTest = clienttasks.getCurrentProductCerts();
		
		// clean up (remove) any extraneous product certs that were installed by the yum productid plugin during testInstallAndRemoveAnyPackageFromEnabledRepoAfterSubscribingToPool
		// Extraneous RHEL product certs are easily possible especially on a HTB installation as explained in https://bugzilla.redhat.com/show_bug.cgi?id=1538957#c5
		for (ProductCert productCertAfterThisTest : productCertsAfterThisTest) {
			for (ProductCert productCertBeforeThisTest : productCertsBeforeThisTest) {
				// do nothing when this productCertAfterTest was also present BeforeTest
				if (productCertBeforeThisTest.file.equals(productCertAfterThisTest.file)) continue;
				// do nothing if this productCertAfterTest is located in the default directory
				if (productCertAfterThisTest.file.getPath().startsWith(clienttasks.productCertDefaultDir)) continue;
				
				// TEMPORARY WORKAROUND
				if (doesStringContainMatches(productCertAfterThisTest.productNamespace.providedTags,"rhel-"+clienttasks.redhatReleaseX+"(,|$)")) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1525238"; // Bug 1525238 - yum plugin for productid neglects to remove HTB product cert from /etc/pki/product/ because it is tagged as a provider of "rhel-7"
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
					   log.warning("Removing product cert '"+productCertAfterThisTest.productName+" "+productCertAfterThisTest.file+"' from restoreProductCertsAfterThisTest() while bug '"+bugId+"' is open.");
					}
				}
				// END OF WORKAROUND
				
				// remove the product cert
			    log.info("Removing product cert '"+productCertAfterThisTest.productName+" "+productCertAfterThisTest.file+"' from restoreProductCertsAfterThisTest().");
				client.runCommandAndWait("rm -f "+productCertAfterThisTest.file);
			}
		}
		productCertsBeforeThisTest.clear();	// clear the protected variable
		productCertsBeforeThisTest=null;
	}

}
