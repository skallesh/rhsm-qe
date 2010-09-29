package com.redhat.qe.sm.base;

import com.redhat.qe.auto.testng.TestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.tools.SSHCommandRunner;

public class SubscriptionManagerBaseTestScript extends TestScript {

	protected String serverHostname			= System.getProperty("rhsm.server.hostname");
	protected String serverPort 			= System.getProperty("rhsm.server.port");
	protected String serverPrefix 			= System.getProperty("rhsm.server.prefix");
	protected String serverBaseUrl			= System.getProperty("rhsm.server.baseurl");
	protected String serverInstallDir		= System.getProperty("rhsm.server.installdir");
	protected String serverImportDir		= System.getProperty("rhsm.server.importdir");
	protected String serverBranch			= System.getProperty("rhsm.server.branch");
	protected Boolean isServerOnPremises	= Boolean.valueOf(System.getProperty("rhsm.server.onpremises","false"));
	protected Boolean deployServerOnPremises= Boolean.valueOf(System.getProperty("rhsm.server.deploy","true"));

	protected String client1hostname		= System.getProperty("rhsm.client1.hostname");
	protected String client1username		= System.getProperty("rhsm.client1.username");
	protected String client1password		= System.getProperty("rhsm.client1.password");

	protected String client2hostname		= System.getProperty("rhsm.client2.hostname");
	protected String client2username		= System.getProperty("rhsm.client2.username");
	protected String client2password		= System.getProperty("rhsm.client2.password");

	protected String clienthostname			= client1hostname;
	protected String clientusername			= client1username;
	protected String clientpassword			= client1password;
	
	protected String clientOwnerUsername	= System.getProperty("rhsm.client.owner.username");
	protected String clientOwnerPassword	= System.getProperty("rhsm.client.owner.password");
	protected String clientUsernames		= System.getProperty("rhsm.client.usernames");
	protected String clientPasswords		= System.getProperty("rhsm.client.passwords");

	protected String tcUnacceptedUsername	= System.getProperty("rhsm.client.username.tcunaccepted");
	protected String tcUnacceptedPassword	= System.getProperty("rhsm.client.password.tcunaccepted");
	protected String regtoken				= System.getProperty("rhsm.client.regtoken");
	protected int certFrequency				= Integer.valueOf(System.getProperty("rhsm.client.certfrequency", "0"));
	protected String enablerepofordeps		= System.getProperty("rhsm.client.enablerepofordeps");

	protected String prodCertLocation		= System.getProperty("rhsm.prodcert.url");
	protected String prodCertProduct		= System.getProperty("rhsm.prodcert.product");
	
	protected String sshUser				= System.getProperty("rhsm.ssh.user","root");
	protected String sshKeyPrivate			= System.getProperty("rhsm.sshkey.private",".ssh/id_auto_dsa");
	protected String sshkeyPassphrase		= System.getProperty("rhsm.sshkey.passphrase","");

//	protected String itDBSQLDriver			= System.getProperty("rhsm.it.db.sqldriver", "oracle.jdbc.driver.OracleDriver");
//	protected String itDBHostname			= System.getProperty("rhsm.it.db.hostname");
//	protected String itDBDatabase			= System.getProperty("rhsm.it.db.database");
//	protected String itDBPort				= System.getProperty("rhsm.it.db.port", "1521");
//	protected String itDBUsername			= System.getProperty("rhsm.it.db.username");
//	protected String itDBPassword			= System.getProperty("rhsm.it.db.password");
	
	protected String dbHostname				= System.getProperty("rhsm.server.db.hostname");
	protected String dbSqlDriver			= System.getProperty("rhsm.server.db.sqldriver");
	protected String dbPort					= System.getProperty("rhsm.server.db.port");
	protected String dbName					= System.getProperty("rhsm.server.db.name");
	protected String dbUsername				= System.getProperty("rhsm.server.db.username");
	protected String dbPassword				= System.getProperty("rhsm.server.db.password");

	protected String[] rpmUrls				= System.getProperty("rhsm.rpm.urls").split(",");
	protected Boolean installRPMs			= Boolean.valueOf(System.getProperty("rhsm.rpm.install","true"));

	public static SSHCommandRunner server	= null;
	public static SSHCommandRunner client	= null;
	public static SSHCommandRunner client1	= null;	// client1
	public static SSHCommandRunner client2	= null;	// client2
	
	protected static CandlepinTasks servertasks	= null;

}
