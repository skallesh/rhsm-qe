package com.redhat.qe.sm.base;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.SubscriptionPool;
import com.redhat.qe.sm.tasks.ModuleTasks;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandRunner;

public class SubscriptionManagerTestScript extends com.redhat.qe.auto.testng.TestScript{
//	protected static final String defaultAutomationPropertiesFile=System.getenv("HOME")+"/sm-tests.properties";
//	public static final String RHSM_LOC = "/usr/sbin/subscription-manager-cli ";
	
	protected String serverHostname			= System.getProperty("rhsm.server.hostname");
	protected String serverPort 			= System.getProperty("rhsm.server.port");
	protected String serverBaseUrl			= System.getProperty("rhsm.server.baseurl");
	protected String serverInstallDir		= System.getProperty("rhsm.server.installdir");
	protected String serverImportDir		= System.getProperty("rhsm.server.importdir");
	protected Boolean isServerOnPremises	= Boolean.valueOf(System.getProperty("rhsm.server.onpremises","false"));

	protected String client1hostname		= System.getProperty("rhsm.client1.hostname");
	protected String client1username		= System.getProperty("rhsm.client1.username");
	protected String client1password		= System.getProperty("rhsm.client1.password");
	
	protected String client2hostname		= System.getProperty("rhsm.client2.hostname");
	protected String client2username		= System.getProperty("rhsm.client2.username");
	protected String client2password		= System.getProperty("rhsm.client2.password");

	protected String clienthostname			= client1hostname;
	protected String clientusername			= client1username;
	protected String clientpassword			= client1password;

	protected String tcUnacceptedUsername	= System.getProperty("rhsm.client.username.tcunaccepted");
	protected String tcUnacceptedPassword	= System.getProperty("rhsm.client.password.tcunaccepted");
	protected String regtoken				= System.getProperty("rhsm.client.regtoken");
	protected int certFrequency				= Integer.valueOf(System.getProperty("rhsm.client.certfrequency"));
	protected String enablerepofordeps		= System.getProperty("rhsm.client.enablerepofordeps");

	protected String prodCertLocation		= System.getProperty("rhsm.prodcert.url");
	protected String prodCertProduct		= System.getProperty("rhsm.prodcert.product");
	
	protected String sshUser				= System.getProperty("rhsm.ssh.user","root");
	protected String sshKeyPrivate			= System.getProperty("rhsm.sshkey.private",".ssh/id_auto_dsa");
	protected String sshkeyPassphrase		= System.getProperty("rhsm.sshkey.passphrase","");

	
	protected String itDBSQLDriver			= System.getProperty("rhsm.it.db.sqldriver", "oracle.jdbc.driver.OracleDriver");
	protected String itDBHostname			= System.getProperty("rhsm.it.db.hostname");
	protected String itDBDatabase			= System.getProperty("rhsm.it.db.database");
	protected String itDBPort				= System.getProperty("rhsm.it.db.port", "1521");
	protected String itDBUsername			= System.getProperty("rhsm.it.db.username");
	protected String itDBPassword			= System.getProperty("rhsm.it.db.password");
	
	protected String rpmLocation			= System.getProperty("rhsm.rpm.url");

	protected String defaultConfigFile		= "/etc/rhsm/rhsm.conf";
	protected String rhsmcertdLogFile		= "/var/log/rhsm/rhsmcertd.log";
	protected String rhsmYumRepoFile		= "/etc/yum/pluginconf.d/rhsmplugin.conf";
	
	public static Connection itDBConnection = null;
	
	public static SSHCommandRunner server	= null;
	public static SSHCommandRunner client	= null;
	public static SSHCommandRunner client1	= null;	// client1
	public static SSHCommandRunner client2	= null;	// client2
	
	protected static ModuleTasks clienttasks	= null;
	protected static ModuleTasks client1tasks	= null;	// client1 subscription manager tasks
	protected static ModuleTasks client2tasks	= null;	// client2 subscription manager tasks
	
	
	public SubscriptionManagerTestScript() {
		super();
		// TODO Auto-generated constructor stub
	}

	@BeforeSuite(groups={"setup"},description="subscription manager set up")
	public void setupBeforeSuite() throws ParseException, IOException{
		SSHCommandRunner[] sshCommandRunners;
		
		client = new SSHCommandRunner(clienthostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
		clienttasks = new com.redhat.qe.sm.tasks.ModuleTasks(client);
		sshCommandRunners= new SSHCommandRunner[]{client};
		
		// will we be connecting to the server?
		if (!(	serverHostname.equals("") || serverHostname.startsWith("$") ||
				serverInstallDir.equals("") || serverInstallDir.startsWith("$") )) {
			server = new SSHCommandRunner(serverHostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);

		} else {
			log.info("Assuming the server is already setup and running.");
		}
		
		// will we be testing multiple clients?
		if (!(	client2hostname.equals("") || client2hostname.startsWith("$") ||
				client2username.equals("") || client2username.startsWith("$") ||
				client2password.equals("") || client2password.startsWith("$") )) {
			client1 = client;
			client1tasks = clienttasks;
			client2 = new SSHCommandRunner(client2hostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
			client2tasks = new com.redhat.qe.sm.tasks.ModuleTasks(client2);
			sshCommandRunners= new SSHCommandRunner[]{client1,client2};
		} else {
			log.info("Multi-client testing will be skipped.");
		}
		
		// setup the server
		if (server!=null && isServerOnPremises) {
			this.deployLatestGitTag(server);
		} 
		
		// setup each client
		for (SSHCommandRunner commandRunner : sshCommandRunners) {
			this.installLatestRPM(commandRunner);
			this.updateSMConfigFile(commandRunner, serverHostname, serverPort);
			this.changeCertFrequency(commandRunner, certFrequency);
			commandRunner.runCommandAndWait("killall -9 yum");
			this.cleanOutAllCerts(commandRunner);
			
			// transfer a copy of the CA Cert from the candlepin server to the client
			// TEMPORARY WORK AROUND TO AVOID ISSUES:
			// https://bugzilla.redhat.com/show_bug.cgi?id=617703 
			// https://bugzilla.redhat.com/show_bug.cgi?id=617303
			/*
			if (server!=null && isServerOnPremises) {
				log.warning("TEMPORARY WORKAROUND...");
				RemoteFileTasks.getFile(server.getConnection(), "/tmp","/etc/candlepin/certs/candlepin-ca.crt");
				RemoteFileTasks.putFile(commandRunner.getConnection(), "/tmp/candlepin-ca.crt", "/tmp/", "0644");
			}
			*/
		}
	}
	
	@AfterSuite(groups={"setup"},description="subscription manager tear down")
	public void teardownAfterSuite() {
		if (client2tasks!=null) client2tasks.unregister_();	// release the entitlements consumed by the current registration
		if (client1tasks!=null) client1tasks.unregister_();	// release the entitlements consumed by the current registration
	}
	
	private void cleanOutAllCerts(SSHCommandRunner sshCommandRunner){
		log.info("Cleaning out certs from /etc/pki/consumer, /etc/pki/entitlement/, /etc/pki/entitlement/product, and /etc/pki/product/");
		
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/consumer/*");
		sshCommandRunner.runCommandAndWait("rm -rf /etc/pki/entitlement/*");
		sshCommandRunner.runCommandAndWait("rm -rf /etc/pki/entitlement/product/*");
		sshCommandRunner.runCommandAndWait("rm -rf /etc/pki/product/*");
	}
	
	public void connectToDatabase(){
		itDBConnection = null; 
		try { 
			// Load the JDBC driver 
			String driverName = this.itDBSQLDriver;
			Class.forName(driverName); 
			// Create a connection to the database
			String serverName = this.itDBHostname;
			String portNumber = this.itDBPort;
			String sid = this.itDBDatabase;
			String url = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
			String username = this.itDBUsername;
			String password = this.itDBPassword;
			itDBConnection = DriverManager.getConnection(url, username, password); 
			} 
		catch (ClassNotFoundException e) { 
			log.warning("Oracle JDBC driver not found!");
		} 
		catch (SQLException e) {
			log.warning("Could not connect to backend IT database!  Traceback:\n" + e.getMessage());
		}
	}
	
	public void getSalesToEngineeringProductBindings(){
		try {
			String products = itDBConnection.nativeSQL("select * from butt;");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			log.info("Database query for Sales-to-Engineering product bindings failed!  Traceback:\n"+e.getMessage());
		}
	}
	
	private void deployLatestGitTag(SSHCommandRunner sshCommandRunner) {

		log.info("Upgrading the server to the latest git tag...");
		Assert.assertEquals(RemoteFileTasks.testFileExists(sshCommandRunner, serverInstallDir),1,"Found the server install directory "+serverInstallDir);

		RemoteFileTasks.searchReplaceFile(sshCommandRunner, "/etc/sudoers", "\\(^Defaults[[:space:]]\\+requiretty\\)", "#\\1");	// Needed to prevent error:  sudo: sorry, you must have a tty to run sudo
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout master; git pull", Integer.valueOf(0), null, "'master'");
//		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git tag | grep -E candlepin-0.0.[[:digit:]]{2}-1 | sort | tail -1", Integer.valueOf(0), "^candlepin", null);	// FIXME: WILL HAVE TO CHANGE THE GREP EXPRESSION TO {3} WHEN THE TAGS HIT 100+  ACTUALLY WE NEED A BETTER WAY TO GET THE LATEST GIT TAG
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git tag | sort -t . -k 3 -n | tail -1", Integer.valueOf(0), "^candlepin", null);
		String latestGitTag = sshCommandRunner.getStdout().trim();
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout "+latestGitTag, Integer.valueOf(0), null, "HEAD is now at .* package \\[candlepin\\] release \\["+latestGitTag.substring(latestGitTag.indexOf("-")+1)+"\\]."); //HEAD is now at 560b098... Automatic commit of package [candlepin] release [0.0.26-1].
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "service postgresql restart", Integer.valueOf(0), "Starting postgresql service:\\s+\\[  OK  \\]", null);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "export IMPORTDIR="+serverImportDir+"; cd "+serverInstallDir+"/proxy; buildconf/scripts/deploy", Integer.valueOf(0), "Initialized!", null);
		/* attempt to use live logging
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait("cd "+serverInstallDir+"/proxy; buildconf/scripts/deploy", true);
			Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0));
			Assert.assertContainsMatch(sshCommandResult.getStdout(), "Initialized!");
		*/
		}
	
	private void installLatestRPM(SSHCommandRunner sshCommandRunner) {

		// verify the subscription-manager client is a rhel 6 machine
		log.info("Verifying prerequisite...  client hostname '"+sshCommandRunner.getConnection().getHostname()+"' is a Red Hat Enterprise Linux .* release 6 machine.");
		Assert.assertEquals(sshCommandRunner.runCommandAndWait("cat /etc/redhat-release | grep -E \"^Red Hat Enterprise Linux .* release 6.*\"").getExitCode(),Integer.valueOf(0),"subscription-manager-cli hostname must be RHEL 6.*");

		log.info("Retrieving latest subscription-manager RPM...");
		String sm_rpm = "/tmp/subscription-manager.rpm";
		sshCommandRunner.runCommandAndWait("rm -f "+sm_rpm);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"wget -O "+sm_rpm+" --no-check-certificate \""+rpmLocation+"\"",Integer.valueOf(0),null,"“"+sm_rpm+"” saved");

		log.info("Uninstalling existing subscription-manager RPM...");
		sshCommandRunner.runCommandAndWait("rpm -e subscription-manager-gnome");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"rpm -q subscription-manager-gnome",Integer.valueOf(1),"package subscription-manager-gnome is not installed",null);
		sshCommandRunner.runCommandAndWait("rpm -e subscription-manager");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"rpm -q subscription-manager",Integer.valueOf(1),"package subscription-manager is not installed",null);
		
		log.info("Installing newest subscription-manager RPM...");
		// using yum localinstall should enable testing on RHTS boxes right off the bat.
		sshCommandRunner.runCommandAndWait("yum -y localinstall "+sm_rpm+" --nogpgcheck --disablerepo=* --enablerepo="+enablerepofordeps);

		log.info("Installed version of subscription-manager RPM...");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"rpm -q subscription-manager",Integer.valueOf(0),"^subscription-manager-\\d.*",null);	// subscription-manager-0.63-1.el6.i686
	}
	
	private void updateSMConfigFile(SSHCommandRunner sshCommandRunner, String hostname, String port){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, defaultConfigFile, "^hostname\\s*=.*$", "hostname="+hostname),
				0,"Updated rhsm config hostname to point to:" + hostname);
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, defaultConfigFile, "^port\\s*=.*$", "port="+port),
				0,"Updated rhsm config port to point to:" + port);
		
		// jsefler - 7/21/2010
		// FIXME DELETEME AFTER FIX FROM <alikins> so, just talked to jsefler and nadathur, we are going to temporarily turn ca verification off, till we get a DEV ca or whatever setup, so we don't break QA at the moment
		// TEMPORARY WORK AROUND TO AVOID ISSUES:
		// https://bugzilla.redhat.com/show_bug.cgi?id=617703 
		// https://bugzilla.redhat.com/show_bug.cgi?id=617303
		/*
		if (isServerOnPremises) {

			log.warning("TEMPORARY WORKAROUND...");
			sshCommandRunner.runCommandAndWait("echo \"candlepin_ca_file = /tmp/candlepin-ca.crt\"  >> "+defaultConfigFile);
		}
		*/
		/* Hi,
		Insecure mode option moved to /etc/rhsm/rhsm.conf file after commandline option(-k, --insecure) failed to gather the popularity votes.

		To enable insecure mode, add the following as a new line to rhsm.conf file
		insecure_mode=t
    

		To disable insecure mode, either remove 'insecure_mode' or set it to any value
		other than 't', 'True', 'true', 1.

		thanks,
		Ajay
		*/
		log.warning("WORKAROUND FOR INSECURITY...");
		sshCommandRunner.runCommandAndWait("echo \"insecure_mode = true\"  >> "+defaultConfigFile);
	}
	
	/**
	 * Update the minutes value for the certFrequency setting in the default /etc/rhsm/rhsm.conf file and restart the rhsmcertd service.
	 * @param sshCommandRunner
	 * @param minutes
	 */
	public void changeCertFrequency(SSHCommandRunner sshCommandRunner, int minutes){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, defaultConfigFile, "^certFrequency\\s*=.*$", "certFrequency="+minutes),
				0,"Updated rhsmd cert refresh frequency to "+minutes+" minutes");
//		sshCommandRunner.runCommandAndWait("mv "+rhsmcertdLogFile+" "+rhsmcertdLogFile+".bak");
//		sshCommandRunner.runCommandAndWait("service rhsmcertd restart");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service rhsmcertd restart",Integer.valueOf(0),"^Starting rhsmcertd "+minutes+"\\[  OK  \\]$",null);
//		Assert.assertEquals(
//				RemoteFileTasks.grepFile(sshCommandRunner,rhsmcertdLogFile, "started: interval = "+frequency),
//				0,"interval reported as "+frequency+" in "+rhsmcertdLogFile);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"tail -2 "+rhsmcertdLogFile,Integer.valueOf(0),"started: interval = "+minutes+" minutes",null);

	}

	public void sleep(long milliseconds) {
		log.info("Sleeping for "+milliseconds+" milliseconds...");
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			log.info("Sleep interrupted!");
		}
	}
	
	public int getRandInt(){
		Random gen = new Random();
		return Math.abs(gen.nextInt());
	}
	
	public void runRHSMCallAsLang(SSHCommandRunner sshCommandRunner, String lang,String rhsmCall){
		sshCommandRunner.runCommandAndWait("export LANG="+lang+"; " + rhsmCall);
	}
	
	public void setLanguage(SSHCommandRunner sshCommandRunner, String lang){
		sshCommandRunner.runCommandAndWait("export LANG="+lang);
	}
	

	// Data Providers ***********************************************************************

	
	@DataProvider(name="getAllAvailableSubscriptionPoolsData")
	public Object[][] getAllAvailableSubscriptionPoolsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAllAvailableSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAllAvailableSubscriptionPoolsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		// assure we are registered
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		if (client2tasks!=null)	{
			client2tasks.unregister();
			client2tasks.register(client2username, client2password, null, null, null, null);
		}
		
		// unsubscribe from all consumed product subscriptions and then assemble a list of all SubscriptionPools
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		if (client2tasks!=null)	{
			client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		}

		// populate a list of all available SubscriptionPools
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			ll.add(Arrays.asList(new Object[]{pool}));		
		}
		
		return ll;
	}
	
	
	protected List<List<Object>> getRegistrationDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// String username, String password, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, Integer exitCode, String stdoutRegex, String stderrRegex
		// 									username,			password,						type,	consumerId,	autosubscribe,	force,			debug,	exitCode,	stdoutRegex,																	stderrRegex
		ll.add(Arrays.asList(new Object[]{	"",					"",								null,	null,		null,			Boolean.TRUE,	null,	null,		"Error: username and password are required to register, try register --help.",	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,		"",								null,	null,		null,			Boolean.TRUE,	null,	null,		"Error: username and password are required to register, try register --help.",	null}));
		ll.add(Arrays.asList(new Object[]{	"",					clientpassword,					null,	null,		null,			Boolean.TRUE,	null,	null,		"Error: username and password are required to register, try register --help.",	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,		String.valueOf(getRandInt()),	null,	null,		null,			Boolean.TRUE,	null,	null,		null,																			"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{	clientusername+"X",	String.valueOf(getRandInt()),	null,	null,		null,			Boolean.TRUE,	null,	null,		null,																			"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,		String.valueOf(getRandInt()),	null,	null,		null,			Boolean.TRUE,	null,	null,		null,																			"Invalid username or password"}));

		// force a successful registration, and then...
		// FIXME: https://bugzilla.redhat.com/show_bug.cgi?id=616065
		ll.add(Arrays.asList(new Object[]{	clientusername,		clientpassword,					null,	null,		null,			Boolean.TRUE,	null,	null,		"[a-f,0-9,\\-]{36} "+clientusername,											null}));	// https://bugzilla.redhat.com/show_bug.cgi?id=616065

		// ... try to register again even though the system is already registered
		ll.add(Arrays.asList(new Object[]{	clientusername,		clientpassword,					null,	null,		null,			Boolean.FALSE,	null,	null,		"This system is already registered. Use --force to override",					null}));

		if (isServerOnPremises) {
			ll.add(Arrays.asList(new Object[]{	"admin",					"admin",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"admin"+"$",						null}));
			ll.add(Arrays.asList(new Object[]{	"testuser1",				"password",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"testuser1"+"$",					null}));
			ll.add(Arrays.asList(new Object[]{	"testuser2",				"password",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"testuser2"+"$",					null}));
		}
		else {	// user data comes from https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/imanage_qe_IT_data_spec
			ll.add(Arrays.asList(new Object[]{	"ewayte",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"ewayte"+"$",						null}));
			ll.add(Arrays.asList(new Object[]{	"sehuffman",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"epgyadmin",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"epgyadmin"+"$",					null}));
			ll.add(Arrays.asList(new Object[]{	"onthebus",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"onthebus"+"$",					null}));
			ll.add(Arrays.asList(new Object[]{	"epgy_bsears",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc$"}));
			ll.add(Arrays.asList(new Object[]{	"Dadaless",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"emmapease",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"aaronwen",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"davidmcmath",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"davidmcmath"+"$",				null}));
			ll.add(Arrays.asList(new Object[]{	"cfairman2",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"cfairman2"+"$",					null}));
			ll.add(Arrays.asList(new Object[]{	"macfariman",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"isu-ardwin",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"isu-paras",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"isuchaos",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"isucnc",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"isu-thewags",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"isu-thewags"+"$",				null}));
			ll.add(Arrays.asList(new Object[]{	"isu-sukhoy",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"isu-sukhoy"+"$",					null}));
			ll.add(Arrays.asList(new Object[]{	"isu-debrm",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc$"}));
			ll.add(Arrays.asList(new Object[]{	"isu-acoster",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"isunpappas",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc$"}));
			ll.add(Arrays.asList(new Object[]{	"isujdwarn",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"isujdwarn"+"$",					null}));
			ll.add(Arrays.asList(new Object[]{	"pascal.catric@a-sis.com",	"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"pascal.catric@a-sis.com"+"$",	null}));
			ll.add(Arrays.asList(new Object[]{	"xeops",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"xeops"+"$",						null}));
			ll.add(Arrays.asList(new Object[]{	"xeops-js",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"xeop-stenjoa",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"xeop-stenjoa"+"$",				null}));
			ll.add(Arrays.asList(new Object[]{	"tmgedp",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"tmgedp"+"$",						null}));
			ll.add(Arrays.asList(new Object[]{	"jmarra",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"jmarra"+"$",						null}));
			ll.add(Arrays.asList(new Object[]{	"nisadmin",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"nisadmin"+"$",					null}));
			ll.add(Arrays.asList(new Object[]{	"darkrider1",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"darkrider1"+"$",					null}));
			ll.add(Arrays.asList(new Object[]{	"test5",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc$"}));
			ll.add(Arrays.asList(new Object[]{	"amy_redhat2",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,													"^The user has been disabled, if this is a mistake, please contact customer service.$"}));
			ll.add(Arrays.asList(new Object[]{	"test_1",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"test_1"+"$",						null}));
			ll.add(Arrays.asList(new Object[]{	"test2",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"test2"+"$",						null}));
			ll.add(Arrays.asList(new Object[]{	"test3",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"^[a-f,0-9,\\-]{36} "+"test3"+"$",						null}));

		}
		return ll;
	}

}
