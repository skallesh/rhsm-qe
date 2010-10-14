package com.redhat.qe.sm.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.redhat.qe.auto.testng.TestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.tools.SSHCommandRunner;

public class SubscriptionManagerBaseTestScript extends TestScript {

	protected String serverHostname			= getProperty("rhsm.server.hostname","");
	protected String serverPort 			= getProperty("rhsm.server.port","");
	protected String serverPrefix 			= getProperty("rhsm.server.prefix","");
	protected String serverBaseUrl			= getProperty("rhsm.server.baseurl","");
	protected String serverInstallDir		= getProperty("rhsm.server.installdir","");
	protected String serverImportDir		= getProperty("rhsm.server.importdir","");
	protected String serverBranch			= getProperty("rhsm.server.branch","");
	protected Boolean isServerOnPremises	= Boolean.valueOf(getProperty("rhsm.server.onpremises","false"));
	protected Boolean deployServerOnPremises= Boolean.valueOf(getProperty("rhsm.server.deploy","true"));

	protected String client1hostname		= getProperty("rhsm.client1.hostname","");
	protected String client1username		= getProperty("rhsm.client1.username","");
	protected String client1password		= getProperty("rhsm.client1.password","");

	protected String client2hostname		= getProperty("rhsm.client2.hostname","");
	protected String client2username		= getProperty("rhsm.client2.username","");
	protected String client2password		= getProperty("rhsm.client2.password","");

	protected String clienthostname			= client1hostname;
	protected String clientusername			= client1username;
	protected String clientpassword			= client1password;
	
	protected String clientOwnerUsername	= getProperty("rhsm.client.owner.username","");
	protected String clientOwnerPassword	= getProperty("rhsm.client.owner.password","");
	protected String clientUsernames		= getProperty("rhsm.client.usernames","");
	protected String clientPasswords		= getProperty("rhsm.client.passwords","");

	protected String tcUnacceptedUsername	= getProperty("rhsm.client.username.tcunaccepted","");
	protected String tcUnacceptedPassword	= getProperty("rhsm.client.password.tcunaccepted","");
	protected String regtoken				= getProperty("rhsm.client.regtoken","");
	protected int certFrequency				= Integer.valueOf(getProperty("rhsm.client.certfrequency", "240"));
	protected String enablerepofordeps		= getProperty("rhsm.client.enablerepofordeps","");

	protected String prodCertLocation		= getProperty("rhsm.prodcert.url","");
	protected String prodCertProduct		= getProperty("rhsm.prodcert.product","");
	
	protected String sshUser				= getProperty("rhsm.ssh.user","root");
	protected String sshKeyPrivate			= getProperty("rhsm.sshkey.private",".ssh/id_auto_dsa");
	protected String sshkeyPassphrase		= getProperty("rhsm.sshkey.passphrase","");

//	protected String itDBSQLDriver			= getProperty("rhsm.it.db.sqldriver", "oracle.jdbc.driver.OracleDriver");
//	protected String itDBHostname			= getProperty("rhsm.it.db.hostname");
//	protected String itDBDatabase			= getProperty("rhsm.it.db.database");
//	protected String itDBPort				= getProperty("rhsm.it.db.port", "1521");
//	protected String itDBUsername			= getProperty("rhsm.it.db.username");
//	protected String itDBPassword			= getProperty("rhsm.it.db.password");
	
	protected String dbHostname				= getProperty("rhsm.server.db.hostname","");
	protected String dbSqlDriver			= getProperty("rhsm.server.db.sqldriver","");
	protected String dbPort					= getProperty("rhsm.server.db.port","");
	protected String dbName					= getProperty("rhsm.server.db.name","");
	protected String dbUsername				= getProperty("rhsm.server.db.username","");
	protected String dbPassword				= getProperty("rhsm.server.db.password","");

	protected List<String> rpmUrls			= Arrays.asList(getProperty("rhsm.rpm.urls", "").trim().split(","));

	public static SSHCommandRunner server	= null;
	public static SSHCommandRunner client	= null;
	public static SSHCommandRunner client1	= null;	// client1
	public static SSHCommandRunner client2	= null;	// client2
	
	protected static CandlepinTasks servertasks	= null;
	
	
	

	
	public SubscriptionManagerBaseTestScript() {
		super();
		// TODO Auto-generated constructor stub
		
		if (rpmUrls.contains("")) rpmUrls = new ArrayList<String>();	// rpmUrls list should be empty when rhsm.rpm.urls is ""
	}
	
	/**
	 * Wraps a call to System.getProperty (String key, String def) returning "" when the System value startsWith("$")
	 * @param key
	 * @param def
	 * @return
	 */
	protected String getProperty (String key, String def) {
		// Hudson parameters that are left blank will be passed in by the variable
		// name of the parameter beginning with a $ sign, catch these and blank it
		String property = System.getProperty(key,def);
		return property.startsWith("$")? "":property;
	}

}
