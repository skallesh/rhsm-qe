package com.redhat.qe.sm.base;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Random;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandRunner;

public class SubscriptionManagerTestScript extends com.redhat.qe.auto.testng.TestScript{
//	protected static final String defaultAutomationPropertiesFile=System.getenv("HOME")+"/sm-tests.properties";
//	public static final String RHSM_LOC = "/usr/sbin/subscription-manager-cli ";
	
	protected String serverHostname			= System.getProperty("rhsm.server.hostname");
	protected String serverPort 			= System.getProperty("rhsm.server.port");
	protected String serverBaseUrl			= System.getProperty("rhsm.server.baseurl");
	protected Boolean serverStandalone		= Boolean.valueOf(System.getProperty("rhsm.server.standalone","false"));

	protected String clientHostname			= System.getProperty("rhsm.client.hostname");
	protected String username				= System.getProperty("rhsm.client.username");
	protected String password				= System.getProperty("rhsm.client.password");
	protected String tcUnacceptedUsername	= System.getProperty("rhsm.client.username.tcunaccepted");
	protected String tcUnacceptedPassword	= System.getProperty("rhsm.client.password.tcunaccepted");
	protected String regtoken				= System.getProperty("rhsm.client.regtoken");
	protected String certFrequency			= System.getProperty("rhsm.client.certfrequency");
	protected String repoForDependencies	= System.getProperty("rhsm.client.repofordependencies");

	protected String prodCertLocation		= System.getProperty("rhsm.prodcert");
	protected String prodCertProduct		= System.getProperty("rhsm.prodcert.product");
	
	protected String clientsshUser			= System.getProperty("rhsm.ssh.user","root");
	protected String clientsshKeyPrivate	= System.getProperty("rhsm.sshkey.private",".ssh/id_auto_dsa");
	protected String clientsshkeyPassphrase	= System.getProperty("rhsm.sshkey.passphrase","");

	
	protected String itDBSQLDriver			= System.getProperty("rhsm.it.db.sqldriver", "oracle.jdbc.driver.OracleDriver");
	protected String itDBHostname			= System.getProperty("rhsm.it.db.hostname");
	protected String itDBDatabase			= System.getProperty("rhsm.it.db.database");
	protected String itDBPort				= System.getProperty("rhsm.it.db.port", "1521");
	protected String itDBUsername			= System.getProperty("rhsm.it.db.username");
	protected String itDBPassword			= System.getProperty("rhsm.it.db.password");
	
	protected String rpmLocation			= System.getProperty("rhsm.rpm");

	protected String defaultConfigFile		= "/etc/rhsm/rhsm.conf";
	protected String rhsmcertdLogFile		= "/var/log/rhsm/rhsmcertd.log";
	protected String rhsmYumRepoFile		= "/etc/yum/pluginconf.d/rhsmplugin.conf";
	
	
	public static SSHCommandRunner sshCommandRunner = null;
	public static Connection itDBConnection = null;
	
	protected static com.redhat.qe.sm.tasks.ModuleTasks sm	= new com.redhat.qe.sm.tasks.ModuleTasks();
	
	
	public SubscriptionManagerTestScript() {
		super();
		// TODO Auto-generated constructor stub
	}

	@BeforeSuite(groups={"sm_setup"},description="subscription manager set up")
	public void setupBeforeSuite() throws ParseException, IOException{
		sshCommandRunner = new SSHCommandRunner(clientHostname, clientsshUser, clientsshKeyPrivate, clientsshkeyPassphrase, null);
		sm.setSSHCommandRunner(sshCommandRunner);
		
		// verify the subscription-manager client is a rhel 6 machine
		log.info("Verifying prerequisite...  client hostname '"+clientHostname+"' is a Red Hat Enterprise Linux .* release 6 machine.");
		Assert.assertEquals(sshCommandRunner.runCommandAndWait("cat /etc/redhat-release | grep -E \"^Red Hat Enterprise Linux .* release 6.*\""),Integer.valueOf(0),"Grinder hostname must be RHEL 6.*");
		
		this.installLatestRPM();
		this.updateSMConfigFile(serverHostname, serverPort);
		this.changeCertFrequency(certFrequency);
		sshCommandRunner.runCommandAndWait("killall -9 yum");
		
//setup should not be running sm commands		sm.unregister();	// unregister after updating the config file
		this.cleanOutAllCerts();	// is this really needed?  shouldn't unregister do this and assert it - jsefler 7/8/2010  - yes it is needed since we should not use sm to unregister here
	}
	
	@AfterSuite(groups={"sm_setup"},description="subscription manager tear down")
	public void teardownAfterSuite() {
		if (sshCommandRunner==null) return;
		if (sm==null) return;
		
		sm.unregister();	// release the entitlements consumed by the current registration
	}
	
	private void cleanOutAllCerts(){
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
	
	private void installLatestRPM(){

		log.info("Retrieving latest subscription-manager RPM...");
		String sm_rpm = "/tmp/subscription-manager.rpm";
		sshCommandRunner.runCommandAndWait("rm -f "+sm_rpm);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"wget -O "+sm_rpm+" --no-check-certificate \""+rpmLocation+"\"",Integer.valueOf(0),null,"“"+sm_rpm+"” saved");

		log.info("Uninstalling existing subscription-manager RPM...");
		sshCommandRunner.runCommandAndWait("rpm -e subscription-manager");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"rpm -q subscription-manager",Integer.valueOf(1),"package subscription-manager is not installed",null);
		sshCommandRunner.runCommandAndWait("rpm -e subscription-manager-gnome");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"rpm -q subscription-manager-gnome",Integer.valueOf(1),"package subscription-manager-gnome is not installed",null);
		
		log.info("Installing newest subscription-manager RPM...");
		// using yum localinstall should enable testing on RHTS boxes right off the bat.
		sshCommandRunner.runCommandAndWait("yum -y localinstall "+sm_rpm+" --nogpgcheck --disablerepo=* --enablerepo="+repoForDependencies);

		log.info("Installed version of subscription-manager RPM...");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"rpm -q subscription-manager",Integer.valueOf(0),"^subscription-manager-\\d.*",null);	// subscription-manager-0.63-1.el6.i686
	}
	
	private void updateSMConfigFile(String hostname, String port){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, defaultConfigFile, "^hostname=.*$", "hostname="+hostname),
				0,"Updated rhsm config hostname to point to:" + hostname);
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, defaultConfigFile, "^port=.*$", "port="+port),
				0,"Updated rhsm config port to point to:" + port);
	}
	
	public void changeCertFrequency(String frequency){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, defaultConfigFile, "^certFrequency=.*$", "certFrequency="+frequency),
				0,"Updated rhsmd cert refresh frequency to "+frequency+" minutes");
		sshCommandRunner.runCommandAndWait("mv "+rhsmcertdLogFile+" "+rhsmcertdLogFile+".bak");
		//sshCommandRunner.runCommandAndWait("service rhsmcertd restart");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service rhsmcertd restart",Integer.valueOf(0),"^Starting rhsmcertd "+frequency+"\\[  OK  \\]$",null);
		Assert.assertEquals(
				RemoteFileTasks.grepFile(sshCommandRunner,rhsmcertdLogFile, "started: interval = "+frequency),
				0,"interval reported as "+frequency+" in "+rhsmcertdLogFile);
	}
	
	public void checkInvalidRegistrationStrings(String username, String password){
		sshCommandRunner.runCommandAndWait("subscription-manager-cli register --username="+username+this.getRandInt()+" --password="+password+this.getRandInt()+" --force");
		Assert.assertContainsMatch(sshCommandRunner.getStdout(),
				"Invalid username or password. To create a login, please visit https:\\/\\/www.redhat.com\\/wapps\\/ugc\\/register.html");
	}
	
	public void sleep(long i) {
		log.info("Sleeping for "+i+" milliseconds...");
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			log.info("Sleep interrupted!");
		}
	}
	
	public int getRandInt(){
		Random gen = new Random();
		return Math.abs(gen.nextInt());
	}
	
	public void runRHSMCallAsLang(String lang,String rhsmCall){
		sshCommandRunner.runCommandAndWait("export LANG="+lang+"; " + rhsmCall);
	}
	
	public void setLanguage(String lang){
		sshCommandRunner.runCommandAndWait("export LANG="+lang);
	}
	

	

}
