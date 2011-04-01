package com.redhat.qe.sm.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.redhat.qe.auto.testng.TestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.tools.SSHCommandRunner;

public class SubscriptionManagerBaseTestScript extends TestScript {

	public static CandlepinTasks servertasks	= null;
	
	public static SSHCommandRunner server	= null;
	public static SSHCommandRunner client	= null;
	public static SSHCommandRunner client1	= null;	// client1
	public static SSHCommandRunner client2	= null;	// client2
	
	// /etc/rhsm/rhsm.conf parameters..................
	
	// rhsm.conf [server] configurations
	public static String serverHostname				= null;
	public static String serverPrefix 				= null;
	public static String serverPort 					= null;
	public static String serverInsecure				= null;
	public static String serverSslVerifyDepth		= null;
	public static String serverCaCertDir				= null;
	
	// rhsm.conf [rhsm] configurations
	public static String rhsmBaseUrl					= null;
	public static String rhsmRepoCaCert				= null;
	//public static String rhsmShowIncompatiblePools	= null;
	public static String rhsmProductCertDir			= null;
	public static String rhsmEntitlementCertDir		= null;
	public static String rhsmConsumerCertDir			= null;
	
	// rhsm.conf [rhsmcertd] configurations
	public static String rhsmcertdCertFrequency		= null;

	
	public String serverAdminUsername		= getProperty("sm.server.admin.username","");
	public String serverAdminPassword		= getProperty("sm.server.admin.password","");
	
	public String serverInstallDir			= getProperty("sm.server.installDir","");
	public String serverImportDir			= getProperty("sm.server.importDir","");
	public String serverBranch				= getProperty("sm.server.branch","");
	public Boolean isServerOnPremises		= Boolean.valueOf(getProperty("sm.server.onPremises","false"));

	public String client1hostname			= getProperty("sm.client1.hostname","");
	public String client1username			= getProperty("sm.client1.username","");
	public String client1password			= getProperty("sm.client1.password","");

	public String client2hostname			= getProperty("sm.client2.hostname","");
	public String client2username			= getProperty("sm.client2.username","");
	public String client2password			= getProperty("sm.client2.password","");

	public String clienthostname			= client1hostname;
	public String clientusername			= client1username;
	public String clientpassword			= client1password;
	
	public String clientUsernames		= getProperty("sm.client.usernames","");
	public String clientPasswords		= getProperty("sm.client.passwords","");
	
	public String usernameWithUnacceptedTC = getProperty("sm.client.username.unacceptedTC","");
	public String passwordWithUnacceptedTC = getProperty("sm.client.password.unacceptedTC","");
	
	public String disabledUsername		= getProperty("sm.client.username.disabled","");
	public String disabledPassword		= getProperty("sm.client.password.disabled","");
	
	public String regtoken				= getProperty("sm.client.regtoken","");
	public String enableRepoForDeps		= getProperty("sm.client.enableRepoForDeps","");
	
	public String sshUser				= getProperty("sm.ssh.user","root");
	public String sshKeyPrivate			= getProperty("sm.sshkey.private",".ssh/id_auto_dsa");
	public String sshkeyPassphrase		= getProperty("sm.sshkey.passphrase","");
	
	public String dbHostname				= getProperty("sm.server.db.hostname","");
	public String dbSqlDriver			= getProperty("sm.server.db.sqlDriver","");
	public String dbPort					= getProperty("sm.server.db.port","");
	public String dbName					= getProperty("sm.server.db.name","");
	public String dbUsername				= getProperty("sm.server.db.username","");
	public String dbPassword				= getProperty("sm.server.db.password","");

	protected List<String> rpmUrls			= null;
	protected List<String> repoCaCertUrls	= null;

//	protected JSONArray systemSubscriptionPoolProductData = null;
	protected JSONArray personSubscriptionPoolProductData = null;
	
	
	

	
	public SubscriptionManagerBaseTestScript() {
		super();
		
		if (getProperty("sm.rpm.urls", "").equals("")) rpmUrls = new ArrayList<String>(); else rpmUrls = Arrays.asList(getProperty("sm.rpm.urls", "").trim().split(" *, *"));
		if (getProperty("sm.rhsm.repoCaCert.urls", "").equals("")) repoCaCertUrls = new ArrayList<String>(); else repoCaCertUrls = Arrays.asList(getProperty("sm.rhsm.repoCaCert.urls", "").trim().split(" *, *"));
		
		// rhsm.conf [server] configurations
		serverHostname				= getProperty("sm.server.hostname","");
		serverPrefix 				= getProperty("sm.server.prefix","");
		serverPort 					= getProperty("sm.server.port","");
		serverInsecure				= getProperty("sm.server.insecure","");
		serverSslVerifyDepth		= getProperty("sm.server.sslVerifyDepth","");
		serverCaCertDir				= getProperty("sm.server.caCertDir","");
		
		// rhsm.conf [rhsm] configurations
		rhsmBaseUrl					= getProperty("sm.rhsm.baseUrl","");
		rhsmRepoCaCert				= getProperty("sm.rhsm.repoCaCert","");
		//rhsmShowIncompatiblePools	= getProperty("sm.rhsm.showIncompatiblePools","");
		rhsmProductCertDir			= getProperty("sm.rhsm.productCertDir","");
		rhsmEntitlementCertDir		= getProperty("sm.rhsm.entitlementCertDir","");
		rhsmConsumerCertDir			= getProperty("sm.rhsm.consumerCertDir","");
		
		// rhsm.conf [rhsmcertd] configurations
		rhsmcertdCertFrequency		= getProperty("sm.rhsmcertd.certFrequency","");

	
		try {
//			systemSubscriptionPoolProductData = new JSONArray(getProperty("sm.system.subscriptionPoolProductData", "<>").replaceAll("<", "[").replaceAll(">", "]")); // hudson parameters use <> instead of []
//			personSubscriptionPoolProductData = new JSONArray(getProperty("sm.person.subscriptionPoolProductData", "<>").replaceAll("<", "[").replaceAll(">", "]")); // hudson parameters use <> instead of []
			personSubscriptionPoolProductData = new JSONArray(getProperty("sm.person.subscriptionPoolProductData", "[]").replaceFirst("^\"", "").replaceFirst("\"$", "")); // hudson JSONArray parameters get surrounded with double quotes that need to be stripped
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}
	
	/**
	 * Wraps a call to System.getProperty (String key, String def) returning def when the System value is undefined or startsWith("$")
	 * @param key
	 * @param def
	 * @return
	 */
	static public String getProperty (String key, String def) {
		// Hudson parameters that are left blank will be passed in by the variable
		// name of the parameter beginning with a $ sign, catch these and blank it
		String property = System.getProperty(key,def);
		return (property.startsWith("$") || property.equals(""))? def:property;
	}

	
	
	public List<String> getPersonProductIds() throws JSONException {
		List<String> personProductIds = new ArrayList<String>();
		for (int j=0; j<personSubscriptionPoolProductData.length(); j++) {
//			try {
				JSONObject poolProductDataAsJSONObject = (JSONObject) personSubscriptionPoolProductData.get(j);
				String personProductId;
				personProductId = poolProductDataAsJSONObject.getString("personProductId");
				personProductIds.add(personProductId);
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		return personProductIds;
	}
}
