package com.redhat.qe.sm.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.redhat.qe.auto.testng.TestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.tools.SSHCommandRunner;

public class SubscriptionManagerBaseTestScript extends TestScript {

	protected String serverHostname			= System.getProperty("rhsm.server.hostname","");
	protected String serverPort 			= System.getProperty("rhsm.server.port","");
	protected String serverPrefix 			= System.getProperty("rhsm.server.prefix","");
	protected String serverBaseUrl			= System.getProperty("rhsm.server.baseurl","");
	protected String serverInstallDir		= System.getProperty("rhsm.server.installdir","");
	protected String serverImportDir		= System.getProperty("rhsm.server.importdir","");
	protected String serverBranch			= System.getProperty("rhsm.server.branch","");
	protected Boolean isServerOnPremises	= Boolean.valueOf(System.getProperty("rhsm.server.onpremises","false"));
	protected Boolean deployServerOnPremises= Boolean.valueOf(System.getProperty("rhsm.server.deploy","true"));

	protected String client1hostname		= System.getProperty("rhsm.client1.hostname","");
	protected String client1username		= System.getProperty("rhsm.client1.username","");
	protected String client1password		= System.getProperty("rhsm.client1.password","");

	protected String client2hostname		= System.getProperty("rhsm.client2.hostname","");
	protected String client2username		= System.getProperty("rhsm.client2.username","");
	protected String client2password		= System.getProperty("rhsm.client2.password","");

	protected String clienthostname			= client1hostname;
	protected String clientusername			= client1username;
	protected String clientpassword			= client1password;
	
	protected String clientOwnerUsername	= System.getProperty("rhsm.client.owner.username","");
	protected String clientOwnerPassword	= System.getProperty("rhsm.client.owner.password","");
	protected String clientUsernames		= System.getProperty("rhsm.client.usernames","");
	protected String clientPasswords		= System.getProperty("rhsm.client.passwords","");

	protected String tcUnacceptedUsername	= System.getProperty("rhsm.client.username.tcunaccepted","");
	protected String tcUnacceptedPassword	= System.getProperty("rhsm.client.password.tcunaccepted","");
	protected String regtoken				= System.getProperty("rhsm.client.regtoken","");
	protected int certFrequency				= Integer.valueOf(System.getProperty("rhsm.client.certfrequency", "240"));
	protected String enablerepofordeps		= System.getProperty("rhsm.client.enablerepofordeps","");

	protected String prodCertLocation		= System.getProperty("rhsm.prodcert.url","");
	protected String prodCertProduct		= System.getProperty("rhsm.prodcert.product","");
	
	protected String sshUser				= System.getProperty("rhsm.ssh.user","root");
	protected String sshKeyPrivate			= System.getProperty("rhsm.sshkey.private",".ssh/id_auto_dsa");
	protected String sshkeyPassphrase		= System.getProperty("rhsm.sshkey.passphrase","");

//	protected String itDBSQLDriver			= System.getProperty("rhsm.it.db.sqldriver", "oracle.jdbc.driver.OracleDriver");
//	protected String itDBHostname			= System.getProperty("rhsm.it.db.hostname");
//	protected String itDBDatabase			= System.getProperty("rhsm.it.db.database");
//	protected String itDBPort				= System.getProperty("rhsm.it.db.port", "1521");
//	protected String itDBUsername			= System.getProperty("rhsm.it.db.username");
//	protected String itDBPassword			= System.getProperty("rhsm.it.db.password");
	
	protected String dbHostname				= System.getProperty("rhsm.server.db.hostname","");
	protected String dbSqlDriver			= System.getProperty("rhsm.server.db.sqldriver","");
	protected String dbPort					= System.getProperty("rhsm.server.db.port","");
	protected String dbName					= System.getProperty("rhsm.server.db.name","");
	protected String dbUsername				= System.getProperty("rhsm.server.db.username","");
	protected String dbPassword				= System.getProperty("rhsm.server.db.password","");

	protected List<String> rpmUrls			= Arrays.asList(System.getProperty("rhsm.rpm.urls", "").trim().split(","));

	public static SSHCommandRunner server	= null;
	public static SSHCommandRunner client	= null;
	public static SSHCommandRunner client1	= null;	// client1
	public static SSHCommandRunner client2	= null;	// client2
	
	protected static CandlepinTasks servertasks	= null;
	
	
	
	
	
	public SubscriptionManagerBaseTestScript() {
		super();
		// TODO Auto-generated constructor stub
		
//		// Hudson parameters that are left blank will be passed in by the variable name of the parameter beginning with a $ sign
//		if (serverHostname.startsWith("$"))		serverHostname = "";
//		if (serverPort.startsWith("$"))			serverPort = "";
//		if (serverPrefix.startsWith("$"))		serverPrefix = "";
//		if (serverBaseUrl.startsWith("$"))		serverBaseUrl = "";
//		if (serverInstallDir.startsWith("$"))	serverInstallDir = "";
//		if (serverImportDir.startsWith("$"))	serverImportDir = "";
//		if (serverBranch.startsWith("$"))		serverBranch = "";
//		if (client1hostname.startsWith("$"))		client1hostname = "";
//		if (client1username.startsWith("$"))		client1username = "";
//		if (client1password.startsWith("$"))		client1password = "";
//		if (client2hostname.startsWith("$"))		client2hostname = "";
//		if (client2username.startsWith("$"))		client2username = "";
//		if (client2password.startsWith("$"))		client2password = "";
//		if (clienthostname.startsWith("$"))		clienthostname = "";
//		if (clientusername.startsWith("$"))		clientusername = "";
//		if (clientpassword.startsWith("$"))		clientpassword = "";
//		if (clientOwnerUsername.startsWith("$"))		clientOwnerUsername = "";
//		if (clientOwnerPassword.startsWith("$"))		clientOwnerPassword = "";
//		if (clientUsernames.startsWith("$"))		clientUsernames = "";
//		if (clientPasswords.startsWith("$"))		clientPasswords = "";
//		if (tcUnacceptedUsername.startsWith("$"))		tcUnacceptedUsername = "";
//		if (tcUnacceptedPassword.startsWith("$"))		tcUnacceptedPassword = "";
//		if (regtoken.startsWith("$"))		regtoken = "";
//		if (enablerepofordeps.startsWith("$"))		enablerepofordeps = "";
//		if (prodCertLocation.startsWith("$"))		prodCertLocation = "";
//		if (prodCertProduct.startsWith("$"))		prodCertProduct = "";
//		if (sshUser.startsWith("$"))		sshUser = "";
//		if (sshKeyPrivate.startsWith("$"))		sshKeyPrivate = "";
//		if (sshkeyPassphrase.startsWith("$"))		sshkeyPassphrase = "";
//		if (dbHostname.startsWith("$"))		dbHostname = "";
//		if (dbSqlDriver.startsWith("$"))		dbSqlDriver = "";
//		if (dbPort.startsWith("$"))		dbPort = "";
//		if (dbName.startsWith("$"))		dbName = "";
//		if (dbUsername.startsWith("$"))		dbUsername = "";
//		if (dbPassword.startsWith("$"))		dbPassword = "";


		if (rpmUrls.contains("")) rpmUrls = new ArrayList<String>();	// rpmUrls list should be empty when rhsm.rpm.urls is ""
		
		
	}

}
