package com.redhat.qe.sm.base;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.jws.Oneway;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.cli.tasks.SubscriptionManagerTasks;
import com.redhat.qe.sm.data.SubscriptionPool;
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
	public void setupBeforeSuite() throws JSONException, Exception{
	
		client = new SSHCommandRunner(clienthostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
		clienttasks = new SubscriptionManagerTasks(client);
		client1 = client;
		client1tasks = clienttasks;
		
		// will we be connecting to the candlepin server?
		if (!(	serverHostname.equals("") || serverInstallDir.equals("") )) {
			server = new SSHCommandRunner(serverHostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
			servertasks = new com.redhat.qe.sm.cli.tasks.CandlepinTasks(server,serverInstallDir);

		} else {
			log.info("Assuming the server is already setup and running.");
		}
		
		// will we be testing multiple clients?
		if (!(	client2hostname.equals("") || client2username.equals("") || client2password.equals("") )) {
			client2 = new SSHCommandRunner(client2hostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
			client2tasks = new SubscriptionManagerTasks(client2);
		} else {
			log.info("Multi-client testing will be skipped.");
		}
		
		// setup the server
		if (server!=null && isServerOnPremises) {
			
			// NOTE: After updating the candlepin.conf file, the server needs to be restarted, therefore this will not work against the Hosted IT server which we don't want to restart or deploy
			//       I suggest manually setting this on hosted and asking calfanso to restart
			servertasks.updateConfigFileParameter("pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule","0 0\\/2 * * * ?");  // every 2 minutes
			servertasks.cleanOutCRL();
			if (deployServerOnPremises) servertasks.deploy(serverHostname, serverImportDir,serverBranch);

			// also connect to the candlepin server database
			connectToDatabase();  // do this after the call to deploy since it will restart postgresql
		}
		
		// in the event that the clients are already registered from a prior run, unregister them
		unregisterClientsAfterSuite();
		
		// setup the client(s)
		for (SubscriptionManagerTasks smt : new SubscriptionManagerTasks[]{client2tasks, client1tasks}) {
			if (smt==null) continue;
			smt.installSubscriptionManagerRPMs(rpmUrls,enableRepoForDeps);
			
			// rhsm.conf [server] configurations
			if (!serverHostname.equals(""))				smt.updateConfigFileParameter("hostname", serverHostname);							else serverHostname = smt.getConfigFileParameter("hostname");
			if (!serverPrefix.equals(""))				smt.updateConfigFileParameter("prefix", serverPrefix);								else serverPrefix = smt.getConfigFileParameter("prefix");
			if (!serverPort.equals(""))					smt.updateConfigFileParameter("port", serverPort);									else serverPort = smt.getConfigFileParameter("port");
			if (!serverInsecure.equals(""))				smt.updateConfigFileParameter("insecure", serverInsecure);							else serverInsecure = smt.getConfigFileParameter("insecure");
			if (!serverCaCertDir.equals(""))			smt.updateConfigFileParameter("ca_cert_dir", serverCaCertDir);						else serverCaCertDir = smt.getConfigFileParameter("ca_cert_dir");

			// rhsm.conf [rhsm] configurations
			if (!rhsmBaseUrl.equals(""))				smt.updateConfigFileParameter("baseurl", rhsmBaseUrl);								else rhsmBaseUrl = smt.getConfigFileParameter("baseurl");
			if (!rhsmRepoCaCert.equals(""))				smt.updateConfigFileParameter("repo_ca_cert", rhsmRepoCaCert);						else rhsmRepoCaCert = smt.getConfigFileParameter("repo_ca_cert");
			if (!rhsmShowIncompatiblePools.equals(""))	smt.updateConfigFileParameter("showIncompatiblePools", rhsmShowIncompatiblePools);	else rhsmShowIncompatiblePools = smt.getConfigFileParameter("showIncompatiblePools");
			if (!rhsmProductCertDir.equals(""))			smt.updateConfigFileParameter("productCertDir", rhsmProductCertDir);				else rhsmProductCertDir = smt.getConfigFileParameter("productCertDir");
			if (!rhsmEntitlementCertDir.equals(""))		smt.updateConfigFileParameter("entitlementCertDir", rhsmEntitlementCertDir);		else rhsmEntitlementCertDir = smt.getConfigFileParameter("entitlementCertDir");
			if (!rhsmConsumerCertDir.equals(""))		smt.updateConfigFileParameter("consumerCertDir", rhsmConsumerCertDir);				else rhsmConsumerCertDir = smt.getConfigFileParameter("consumerCertDir");

			// rhsm.conf [rhsmcertd] configurations
			if (!rhsmcertdCertFrequency.equals(""))		smt.updateConfigFileParameter("certFrequency", rhsmcertdCertFrequency);				else rhsmcertdCertFrequency = smt.getConfigFileParameter("certFrequency");
		
			smt.initializeFieldsFromConfigFile();
			
			
			// FIXME WORKAROUND FOR ALPHA TESTING  DELETEME AFTER ALPHA TESTING IS COMPLETE
			if (!isServerOnPremises) {
				log.warning("FIXME: Ignoring change from https://bugzilla.redhat.com/show_bug.cgi?id=645115 FOR ALPHA TESTING");
				smt.entitlementCertDir += "/product";
			}
			
			
			smt.removeAllCerts(true,true);
		}
//		client1tasks.installSubscriptionManagerRPMs(rpmUrls,enablerepofordeps);
//		client1tasks.updateConfigFileParameter("hostname", serverHostname);
//		client1tasks.updateConfigFileParameter("port", serverPort);
//		client1tasks.updateConfigFileParameter("prefix", serverPrefix);
//		client1tasks.updateConfigFileParameter("baseurl", rhsmBaseUrl);
//		client1tasks.updateConfigFileParameter("insecure", serverInsecure);
////		client1tasks.restart_rhsmcertd(certFrequency,false);
//		client1tasks.removeAllCerts(true,true);
//		if (client2tasks!=null) client2tasks.installSubscriptionManagerRPMs(rpmUrls,enablerepofordeps);
//		if (client2tasks!=null) client2tasks.updateConfigFileParameter("hostname", serverHostname);
//		if (client2tasks!=null) client2tasks.updateConfigFileParameter("port", serverPort);
//		if (client2tasks!=null) client2tasks.updateConfigFileParameter("prefix", serverPrefix);
//		if (client2tasks!=null) client2tasks.updateConfigFileParameter("baseurl", rhsmBaseUrl);
//		if (client2tasks!=null) client2tasks.updateConfigFileParameter("insecure", serverInsecure);
////		if (client2tasks!=null) client2tasks.restart_rhsmcertd(certFrequency,false);
//		if (client2tasks!=null) client2tasks.removeAllCerts(true,true);
		
		// transfer a copy of the CA Cert from the candlepin server to the clients so we can test in secure mode
		if (server!=null && isServerOnPremises) {
			log.info("Copying Candlepin cert onto clients to enable certificate validation...");
			RemoteFileTasks.getFile(server.getConnection(), "/tmp","/etc/candlepin/certs/candlepin-ca.crt");
			
			RemoteFileTasks.putFile(client1.getConnection(), "/tmp/candlepin-ca.crt", client1tasks.getConfigFileParameter("ca_cert_dir")+"/"+serverHostname.split("\\.")[0]+"-candlepin-ca.pem", "0644");
			client1tasks.updateConfigFileParameter("insecure", "0");
			if (client2!=null) RemoteFileTasks.putFile(client2.getConnection(), "/tmp/candlepin-ca.crt", client2tasks.getConfigFileParameter("ca_cert_dir")+"/"+serverHostname.split("\\.")[0]+"-candlepin-ca.pem", "0644");
			if (client2!=null) client2tasks.updateConfigFileParameter("insecure", "0");
		}
		
		
		log.info("Installed version of candlepin...");
		JSONObject jsonStatus = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,clientOwnerUsername,clientOwnerPassword,"/status"));
		log.info("Candlepin server '"+serverHostname+"' is running version: "+jsonStatus.get("version"));
		
		log.info("Installed version of subscription-manager...");
		log.info("Subscription manager client '"+client1hostname+"' is running version: "+client1.runCommandAndWait("rpm -q subscription-manager").getStdout()); // subscription-manager-0.63-1.el6.i686
		if (client2!=null) log.info("Subscription manager client '"+client2hostname+"' is running version: "+client2.runCommandAndWait("rpm -q subscription-manager").getStdout()); // subscription-manager-0.63-1.el6.i686

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
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
		if (client2tasks!=null)	{
			client2tasks.unregister();
			client2tasks.register(client2username, client2password, null, null, null, null, null);
		}
		
		// unsubscribe from all consumed product subscriptions and then assemble a list of all SubscriptionPools
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		if (client2tasks!=null)	{
			client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		}

		// populate a list of all available SubscriptionPools
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
// FIXME CLEAR USED FOR FASTER TESTING
//ll.clear();
			ll.add(Arrays.asList(new Object[]{pool}));		
		}
		return ll;
	}
	

}
