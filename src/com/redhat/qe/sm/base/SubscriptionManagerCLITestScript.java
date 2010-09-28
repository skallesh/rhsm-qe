package com.redhat.qe.sm.base;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.sm.tasks.CandlepinTasks;
import com.redhat.qe.sm.tasks.SubscriptionManagerTasks;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
public class SubscriptionManagerCLITestScript extends SubscriptionManagerBaseTestScript{
//	protected static final String defaultAutomationPropertiesFile=System.getenv("HOME")+"/sm-tests.properties";
//	public static final String RHSM_LOC = "/usr/sbin/subscription-manager-cli ";
	


//DELETEME
//MOVED TO TASKS CLASSES
//	protected String defaultConfigFile		= "/etc/rhsm/rhsm.conf";
//	protected String rhsmcertdLogFile		= "/var/log/rhsm/rhsmcertd.log";
//	protected String rhsmYumRepoFile		= "/etc/yum/pluginconf.d/rhsmplugin.conf";
	
//	public static Connection itDBConnection = null;
	public static Connection dbConnection = null;
	
	protected static SubscriptionManagerTasks clienttasks	= null;
	protected static SubscriptionManagerTasks client1tasks	= null;	// client1 subscription manager tasks
	protected static SubscriptionManagerTasks client2tasks	= null;	// client2 subscription manager tasks
	
	protected Random randomGenerator = new Random(System.currentTimeMillis());
	
	public SubscriptionManagerCLITestScript() {
		super();
		// TODO Auto-generated constructor stub
	}


	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeSuite(groups={"setup"},description="subscription manager set up")
	public void setupBeforeSuite() throws ParseException, IOException{
	
		client = new SSHCommandRunner(clienthostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
		clienttasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client);
		
		// will we be connecting to the candlepin server?
		if (!(	serverHostname.equals("") || serverHostname.startsWith("$") ||
				serverInstallDir.equals("") || serverInstallDir.startsWith("$") )) {
			server = new SSHCommandRunner(serverHostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
			servertasks = new com.redhat.qe.sm.tasks.CandlepinTasks(server,serverInstallDir);

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
			client2tasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client2);
		} else {
			log.info("Multi-client testing will be skipped.");
		}
		
		// setup the server
		if (server!=null && isServerOnPremises) {
			servertasks.updateConfigFileParameter("pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule","0 0\\/2 * * * ?");
			servertasks.cleanOutCRL();
			if (deployServerOnPremises)
				servertasks.deploy(serverHostname, serverImportDir,serverBranch);
			
			// also connect to the candlepin server database
			connectToDatabase();  // do this after the call to deploy since it will restart postgresql
		}
		
		// in the event that the clients are already registered from a prior run, unregister them
		unregisterClientsAfterSuite();
		
		// setup the client(s)
		if (installRPM) client1tasks.installSubscriptionManagerRPM(urlToRPM,enablerepofordeps);
		client1tasks.updateConfigFileParameter("hostname", serverHostname);
		client1tasks.updateConfigFileParameter("port", serverPort);
		client1tasks.updateConfigFileParameter("prefix", serverPrefix);
		client1tasks.updateConfigFileParameter("insecure", "1");
		client1tasks.changeCertFrequency(certFrequency,false);
		client1tasks.cleanOutAllCerts();
		if (client2tasks!=null) if (installRPM) client2tasks.installSubscriptionManagerRPM(urlToRPM,enablerepofordeps);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("hostname", serverHostname);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("port", serverPort);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("prefix", serverPrefix);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("insecure", "1");
		if (client2tasks!=null) client2tasks.changeCertFrequency(certFrequency,false);
		if (client2tasks!=null) client2tasks.cleanOutAllCerts();
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
	
	@AfterSuite(groups={"setup"},description="subscription manager tear down")
	public void unregisterClientsAfterSuite() {
		if (client2tasks!=null) client2tasks.unregister_();	// release the entitlements consumed by the current registration
		if (client1tasks!=null) client1tasks.unregister_();	// release the entitlements consumed by the current registration
	}
	
	@AfterSuite(groups={"setup"},description="subscription manager tear down")
	public void disconnectDatabaseAfterSuite() {
		
		// close the candlepin database connection
		if (dbConnection!=null) {
			try {
				dbConnection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	// close the connection to the database
		}
	}


	
	// Protected Methods ***********************************************************************
	
	protected void connectToDatabase() {
		try { 
			// Load the JDBC driver 
			Class.forName(dbSqlDriver);	//	"org.postgresql.Driver" or "oracle.jdbc.driver.OracleDriver"
			
			// Create a connection to the database
			String url = dbSqlDriver.contains("postgres")? 
					"jdbc:postgresql://" + dbHostname + ":" + dbPort + "/" + dbName :
					"jdbc:oracle:thin:@" + dbHostname + ":" + dbPort + ":" + dbName ;
			log.info(String.format("Attempting to connect to database with url and credentials: url=%s username=%s password=%s",url,dbUsername,dbPassword));
			dbConnection = DriverManager.getConnection(url, dbUsername, dbPassword); 
			DatabaseMetaData dbmd = dbConnection.getMetaData(); //get MetaData to confirm connection
		    log.fine("Connection to "+dbmd.getDatabaseProductName()+" "+dbmd.getDatabaseProductVersion()+" successful.\n");

		} 
		catch (ClassNotFoundException e) { 
			log.warning("JDBC driver not found!:\n" + e.getMessage());
		} 
		catch (SQLException e) {
			log.warning("Could not connect to backend database:\n" + e.getMessage());
		}
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
	
	protected int getRandInt(){
		return Math.abs(randomGenerator.nextInt());
	}
	
	
//	public void runRHSMCallAsLang(SSHCommandRunner sshCommandRunner, String lang,String rhsmCall){
//		sshCommandRunner.runCommandAndWait("export LANG="+lang+"; " + rhsmCall);
//	}
//	
//	public void setLanguage(SSHCommandRunner sshCommandRunner, String lang){
//		sshCommandRunner.runCommandAndWait("export LANG="+lang);
//	}
	

	// Protected Inner Data Class ***********************************************************************
	
	protected class RegistrationData {
		public String username=null;
		public String password=null;
		public JSONObject jsonOwner=null;
		public SSHCommandResult registerResult=null;
		public List<SubscriptionPool> allAvailableSubscriptionPools=null;/*new ArrayList<SubscriptionPool>();*/
		public RegistrationData() {
			super();
		}
		public RegistrationData(String username, String password, JSONObject jsonOwner,	SSHCommandResult registerResult, List<SubscriptionPool> allAvailableSubscriptionPools) {
			super();
			this.username = username;
			this.password = password;
			this.jsonOwner = jsonOwner;
			this.registerResult = registerResult;
			this.allAvailableSubscriptionPools = allAvailableSubscriptionPools;
		}
	}
	
	// this list will be populated by subclass ResisterTests.RegisterWithUsernameAndPassword_Test
	protected List<RegistrationData> registrationDataList = new ArrayList<RegistrationData>();	

	
	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="getGoodRegistrationData")
	public Object[][] getGoodRegistrationDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getGoodRegistrationDataAsListOfLists());
	}
	protected List<List<Object>> getGoodRegistrationDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
//		for (List<Object> registrationDataList : getBogusRegistrationDataAsListOfLists()) {
//			// pull out all of the valid registration data (indicated by an Integer exitCode of 0)
//			if (registrationDataList.contains(Integer.valueOf(0))) {
//				// String username, String password, String type, String consumerId
//				ll.add(registrationDataList.subList(0, 4));
//			}
//			
//		}
// changing to registrationDataList to get all the valid registeredConsumer
		
		for (RegistrationData registeredConsumer : registrationDataList) {
			if (registeredConsumer.registerResult.getExitCode().intValue()==0) {
				ll.add(Arrays.asList(new Object[]{registeredConsumer.username, registeredConsumer.password}));
			}
		}
		
		return ll;
	}
	
	
	@DataProvider(name="getAvailableSubscriptionPoolsData")
	public Object[][] getAvailableSubscriptionPoolsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableSubscriptionPoolsDataAsListOfLists() {
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
	

}
