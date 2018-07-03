package rhsm.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestScript;
import rhsm.cli.tasks.CandlepinTasks;
import com.redhat.qe.tools.SSHCommandRunner;

public class SubscriptionManagerBaseTestScript extends TestScript {

	public static CandlepinTasks servertasks	= null;
	
	public static SSHCommandRunner server	= null;
	public static SSHCommandRunner client	= null;
	public static SSHCommandRunner client1	= null;	// client1
	public static SSHCommandRunner client2	= null;	// client2
	
	// /etc/rhsm/rhsm.conf [server] configurations
	public static String sm_serverHostname			= null;
	public static String sm_serverPrefix 			= null;
	public static String sm_serverPort 				= null;
	public static String sm_serverInsecure			= null;
	public static String sm_serverSslVerifyDepth	= null;
	public static String sm_serverCaCertDir			= null;
	
	// /etc/rhsm/rhsm.conf [rhsm] configurations
	public static String sm_rhsmBaseUrl				= null;
	public static String sm_rhsmRepoCaCert			= null;
	public static String sm_rhsmProductCertDir		= null;
	public static String sm_rhsmEntitlementCertDir	= null;
	public static String sm_rhsmConsumerCertDir		= null;
	
	// /etc/rhsm/rhsm.conf [rhsmcertd] configurations
	public static String sm_rhsmcertdCertFrequency	= null;
	public static String sm_rhsmcertdHealFrequency	= null;
	
	public static String sm_serverUrl				= null;
	
	// IF THE FOLLOWING VARIABLES ARE INITIALIZED USING getProperty(...),
	// DO NOT MAKE THEM static BECAUSE THE GUI CLOJURE TESTS WILL HAVE
	// DIFFICULTING LOADING THEM UNLESS THEY ARE DEFINED BY -Dsm.foo=bar
	// SYNTAX ON THE java COMMAND LINE
	
	public final String jenkinsUsername			= getProperty("jenkins.username","");
	public final String jenkinsPassword			= getProperty("jenkins.password","");
	
	public final String polarionPlannedIn		= getProperty("polarion.planned_in","None");
	
	public final String sm_serverAdminUsername	= getProperty("sm.server.admin.username","");
	public final String sm_serverAdminPassword	= getProperty("sm.server.admin.password","");

	public String sm_serverSSHUser				= getProperty("sm.server.sshUser","root");
	public String sm_serverInstallDir			= getProperty("sm.server.installDir","");
	public String sm_serverImportDir			= getProperty("sm.server.importDir","");
	public String sm_serverBranch				= getProperty("sm.server.branch","");
	public CandlepinType sm_serverType			= CandlepinType.valueOf(getProperty("sm.server.type","standalone"));
	public Boolean sm_serverOld					= Boolean.valueOf(getProperty("sm.server.old","false"));
	
	public String sm_client1SSHUser				= getProperty("sm.client1.sshUser","root");
	public String sm_client1Hostname			= getProperty("sm.client1.hostname","");
	public String sm_client1Username			= getProperty("sm.client1.username",null);
	public String sm_client1Password			= getProperty("sm.client1.password",null);
	public String sm_client1Org					= getProperty("sm.client1.org",null);

	public String sm_client2SSHUser				= getProperty("sm.client2.sshUser","root");
	public String sm_client2Hostname			= getProperty("sm.client2.hostname","");
	public String sm_client2Username			= getProperty("sm.client2.username",null);
	public String sm_client2Password			= getProperty("sm.client2.password",null);
	public String sm_client2Org					= getProperty("sm.client2.org",null);

	public String sm_clientSSHUser				= sm_client1SSHUser;
	public String sm_clientHostname				= sm_client1Hostname;
	public String sm_clientUsername				= sm_client1Username;
	public String sm_clientPassword				= sm_client1Password;
	public String sm_clientOrg					= sm_client1Org;
	
	public String sm_clientCertificateVersion	= getProperty("sm.client.certificateVersion",null);
	public Boolean sm_clientFips				= Boolean.valueOf(getProperty("sm.client.fips","false"));


// TODO RE-IMPLEMENT AS AN ALTERNATIVE FOR USE DURING STAGE TESTING
//	public String sm_clientUsernames			= getProperty("sm.client.usernames","");
//	public String sm_clientPasswords			= getProperty("sm.client.passwords","");
	public String sm_clientPasswordDefault		= getProperty("sm.client.passwordDefault","MISSING_PROPERTY sm.client.passwordDefault");
	
	public String sm_rhpersonalUsername			= getProperty("sm.rhpersonal.username", "");
	public String sm_rhpersonalPassword			= getProperty("sm.rhpersonal.password", "");
	public String sm_rhpersonalOrg				= getProperty("sm.rhpersonal.org", null);
	public String sm_rhpersonalSubproductQuantity =	getProperty("sm.rhpersonal.subproductQuantity", "unlimited");
	
	public String sm_rhuiSubscriptionProductId	= getProperty("sm.rhui.subscriptionProductId", "");
	public String sm_rhuiRepoIdForIsos			= getProperty("sm.rhui.repoIdForIsos", "");
	public String sm_rhuiDownloadIso			= getProperty("sm.rhui.downloadIso", "");
	
	public String sm_haUsername					= getProperty("sm.ha.username", "");
	public String sm_haPassword					= getProperty("sm.ha.password", "");
	public String sm_haOrg						= getProperty("sm.ha.org", null);
//	public String sm_haSku						= getProperty("sm.ha.sku", "");
	
	public String sm_usernameWithUnacceptedTC	= getProperty("sm.client.username.unacceptedTC","");
	public String sm_passwordWithUnacceptedTC	= getProperty("sm.client.password.unacceptedTC","");
	
	public String sm_disabledUsername			= getProperty("sm.client.username.disabled","");
	public String sm_disabledPassword			= getProperty("sm.client.password.disabled","");
	
	public String sm_regtoken					= getProperty("sm.client.regtoken","");
	public String sm_yumInstallOptions			= getProperty("sm.client.yumInstallOptions",""/*"--nogpgcheck"*/);
	public Boolean sm_yumInstallZStreamUpdates	= Boolean.valueOf(getProperty("sm.client.yumInstallZStreamUpdates","false"));
	public String sm_yumInstallZStreamComposeUrl= getProperty("sm.client.yumInstallZStreamComposeUrl","http://UNSPECIFIED/");
	public String sm_yumInstallZStreamBrewUrl	= getProperty("sm.client.yumInstallZStreamBrewUrl","http://UNSPECIFIED/");
	public String sm_ciMessage					= System.getenv("CI_MESSAGE");	// comes from a CI event Build Trigger in Jenkins 
	
	public String sm_sshKeyPrivate				= getProperty("sm.sshkey.private",".ssh/id_auto_dsa");
	public String sm_sshkeyPassphrase			= getProperty("sm.sshkey.passphrase","");
	public String sm_sshEmergenecyTimeoutMS		= getProperty("sm.sshEmergencyTimeoutMS",null);
	
	public String sm_dbHostname					= getProperty("sm.server.db.hostname","");
	public String sm_dbSqlDriver				= getProperty("sm.server.db.sqlDriver","");
	public String sm_dbPort						= getProperty("sm.server.db.port","");
	public String sm_dbName						= getProperty("sm.server.db.name","");
	public String sm_dbUsername					= getProperty("sm.server.db.username","");
	public String sm_dbPassword					= getProperty("sm.server.db.password","");
	
	public String sm_productCertValidityDuration	= getProperty("sm.client.productCertValidityDuration", "");
	
	// a basicauth proxy requires a username and password
	public String sm_basicauthproxySSHUser		= getProperty("sm.basicauthproxy.sshUser","root");
	public String sm_basicauthproxyHostname		= getProperty("sm.basicauthproxy.hostname", "");
	public String sm_basicauthproxyPort			= getProperty("sm.basicauthproxy.port", "");
	public String sm_basicauthproxyUsername		= getProperty("sm.basicauthproxy.username", "");
	public String sm_basicauthproxyPassword		= getProperty("sm.basicauthproxy.password", "");
	public String sm_basicauthproxyLog			= getProperty("sm.basicauthproxy.log", "");

	// an unauth proxy will return a 401 Unauthorized response
	public String sm_unauthproxySSHUser			= getProperty("sm.unauthproxy.sshUser","root");
	public String sm_unauthproxyHostname		= getProperty("sm.unauthproxy.hostname", "");
	public String sm_unauthproxyPort			= getProperty("sm.unauthproxy.port", "");
	public String sm_unauthproxyUsername		= getProperty("sm.unauthproxy.username", "");
	public String sm_unauthproxyPassword		= getProperty("sm.unauthproxy.password", "");
	public String sm_unauthproxyLog				= getProperty("sm.unauthproxy.log", "");

	// a noauth proxy does not require any username and password
	public String sm_noauthproxySSHUser			= getProperty("sm.noauthproxy.sshUser","root");
	public String sm_noauthproxyHostname		= getProperty("sm.noauthproxy.hostname", "");
	public String sm_noauthproxyPort			= getProperty("sm.noauthproxy.port", "");
	public String sm_noauthproxyLog				= getProperty("sm.noauthproxy.log", "");
	
	public String sm_rhnUsername				= getProperty("sm.rhn.username","");
	public String sm_rhnPassword				= getProperty("sm.rhn.password","");
	public String sm_rhnHostname				= getProperty("sm.rhn.hostname","");
	
	public String sm_manifestsUrl				= getProperty("sm.manifests.url","");
	public String sm_testpluginsUrl				= getProperty("sm.testplugins.url","");
	
	//public String sm_cdnProductBaselineUrl			= getProperty("sm.cdn.productBaselineUrl","");
	public String sm_rhnDefinitionsGitRepository		= getProperty("sm.rhn.definitionsGitRepository","");
	public String sm_rhnDefinitionsProductBaselineFile	= getProperty("sm.rhn.definitionsProductBaselineFile","");
	public String sm_rhnDefinitionsProductCertsFile		= getProperty("sm.rhn.definitionsProductCertsFile","");

	//public String sm_translateToolkitGitRepository	= getProperty("sm.translate.toolkitGitRepository","");
	public String sm_translateToolkitTarUrl				= getProperty("sm.translate.toolkitTarUrl","");

	protected List<String> sm_exemptServiceLevelsInUpperCase	= new ArrayList<String>();	// TODO Get rid of this field.  Use Candlepin API to fetch this list.  See Bug 1066088 - [RFE] expose an option to the servicelevels api to return exempt service levels	// Use CandlepinTasks.getServiceLevelsForOrgKey(sm_clientUsername, sm_clientPassword, sm_serverUrl, clienttasks.getCurrentlyRegisteredOwnerKey(), true);
	protected List<String> sm_rhnDefinitionsProductCertsDirs	= new ArrayList<String>();
	protected List<String> sm_clientUsernames					= new ArrayList<String>();
	protected List<String> sm_rpmInstallUrls					= new ArrayList<String>();
	protected List<String> sm_rpmUpdateUrls						= new ArrayList<String>();
	protected List<String> sm_repoCaCertUrls					= new ArrayList<String>();
//	protected List<String> sm_haPackages						= new ArrayList<String>();
	protected List<String> sm_dockerRpmInstallUrls				= new ArrayList<String>();
	protected List<String> sm_dockerImages						= new ArrayList<String>();
	protected List<String> sm_yumInstallZStreamUpdatePackages	= new ArrayList<String>();
	
//	protected JSONArray systemSubscriptionPoolProductData = null;
	protected JSONArray sm_personSubscriptionPoolProductData = null;
	protected JSONArray sm_contentIntegrationTestData = null;
	
	
	public SubscriptionManagerBaseTestScript() {
		super();
		
		// flatten all the ConsumerType values into a comma separated list
		String consumerTypesAsString = "";
		for (ConsumerType type : ConsumerType.values()) consumerTypesAsString+=type+",";
		consumerTypesAsString = consumerTypesAsString.replaceAll(",$", "");
		
		if (!getProperty("sm.exemptServiceLevels", "").equals(""))	for (String s : Arrays.asList(getProperty("sm.exemptServiceLevels", "").trim().split(" *, *")))		sm_exemptServiceLevelsInUpperCase.add(s.toUpperCase());	// change to UPPER CASE since these will be case insensitive	// this initialization method allows the list to grow
		if (!getProperty("sm.rhn.definitionsProductCertsDirs", "").equals("")) 	sm_rhnDefinitionsProductCertsDirs	= Arrays.asList(getProperty("sm.rhn.definitionsProductCertsDirs", "").trim().split(" *, *"));
		if (!getProperty("sm.client.usernames", "").equals("")) 				sm_clientUsernames					= Arrays.asList(getProperty("sm.client.usernames", "").trim().split(" *, *"));
		if (!getProperty("sm.rpm.installurls", "").equals("")) 					sm_rpmInstallUrls					= Arrays.asList(getProperty("sm.rpm.installurls", "").trim().split(" *, *"));
		if (!getProperty("sm.rpm.updateurls", "").equals("")) 					sm_rpmUpdateUrls					= Arrays.asList(getProperty("sm.rpm.updateurls", "").trim().split(" *, *"));
		if (!getProperty("sm.rhsm.repoCaCert.urls", "").equals(""))				sm_repoCaCertUrls					= Arrays.asList(getProperty("sm.rhsm.repoCaCert.urls", "").trim().split(" *, *"));
//		if (!getProperty("sm.ha.packages", "").equals(""))						sm_haPackages						= Arrays.asList(getProperty("sm.ha.packages", "").trim().split(" *, *"));
//		if (!getProperty("sm.docker.images", "").equals("")) 					sm_dockerImages						= Arrays.asList(getProperty("sm.docker.images", "").trim().split(" *, *"));
		if (!getProperty("sm.client.yumInstallZStreamUpdatePackages", "").equals(""))						sm_yumInstallZStreamUpdatePackages						= Arrays.asList(getProperty("sm.client.yumInstallZStreamUpdatePackages", "").trim().split(" *, *")); // default of "" implies update every package

//		if (sm_yumInstallZStreamUpdates) 										sm_yumInstallOptions += " --enablerepo=rhel-zstream";
		
		
		// TEMPORARY WORKAROUND
		// find the list index for "dnf-plugin-subscription-manager" and move it to the end of the list while Bug 1581410 is open
		List<String> rpmInstallUrls = new ArrayList<String>();
		int j=-1;
		for (int i = 0; i < sm_rpmInstallUrls.size(); i++) {
			String rpmInstallUrl = sm_rpmInstallUrls.get(i);
			rpmInstallUrls.add(rpmInstallUrl);
			if (rpmInstallUrl.contains("dnf-plugin-subscription-manager")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				invokeWorkaroundWhileBugIsOpen = false; // due to https://bugzilla.redhat.com/show_bug.cgi?id=1581410#c1
				String bugId="1581410";	// Bug 1581410 - package subscription-manager should require dnf-plugin-subscription-manager on RHEL8
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					j=i;
				}
			}
		}
		if (j>=0) {
			rpmInstallUrls.add(rpmInstallUrls.remove(j));
			sm_rpmInstallUrls = rpmInstallUrls;
		}
		// END OF WORKAROUND
		
		if (sm_serverUrl==null)
			sm_serverUrl					= getProperty("sm.server.url","");
		
		// rhsm.conf [server] configurations
		if (sm_serverHostname==null)
			sm_serverHostname				= getProperty("sm.server.hostname","");
		if (sm_serverPrefix==null)
			sm_serverPrefix 				= getProperty("sm.server.prefix","");
		if (sm_serverPort==null)
			sm_serverPort 					= getProperty("sm.server.port","");
		if (sm_serverInsecure==null)
			sm_serverInsecure				= getProperty("sm.server.insecure","");
		if (sm_serverSslVerifyDepth==null)
			sm_serverSslVerifyDepth			= getProperty("sm.server.sslVerifyDepth","");
		if (sm_serverCaCertDir==null)
			sm_serverCaCertDir				= getProperty("sm.server.caCertDir","");
		
		// rhsm.conf [rhsm] configurations
		if (sm_rhsmBaseUrl==null)
			sm_rhsmBaseUrl					= getProperty("sm.rhsm.baseUrl","");
		if (sm_rhsmRepoCaCert==null)
			sm_rhsmRepoCaCert				= getProperty("sm.rhsm.repoCaCert","");
		if (sm_rhsmProductCertDir==null)
			sm_rhsmProductCertDir			= getProperty("sm.rhsm.productCertDir","");
		if (sm_rhsmEntitlementCertDir==null)
			sm_rhsmEntitlementCertDir		= getProperty("sm.rhsm.entitlementCertDir","");
		if (sm_rhsmConsumerCertDir==null)
			sm_rhsmConsumerCertDir			= getProperty("sm.rhsm.consumerCertDir","");
		
		// rhsm.conf [rhsmcertd] configurations
		if (sm_rhsmcertdCertFrequency==null)
			sm_rhsmcertdCertFrequency		= getProperty("sm.rhsmcertd.certFrequency","");
		if (sm_rhsmcertdHealFrequency==null)
			sm_rhsmcertdHealFrequency		= getProperty("sm.rhsmcertd.healFrequency","");
		
		// getting a CI_MESSAGE from the java properties will allow us to test a fake message independent of the ci-trigger Jenkins plugin
		if (sm_ciMessage==null)
			sm_ciMessage					= getProperty("CI_MESSAGE",null); 
		
		try {
//			systemSubscriptionPoolProductData = new JSONArray(getProperty("sm.system.subscriptionPoolProductData", "<>").replaceAll("<", "[").replaceAll(">", "]")); // hudson parameters use <> instead of []
//			personSubscriptionPoolProductData = new JSONArray(getProperty("sm.person.subscriptionPoolProductData", "<>").replaceAll("<", "[").replaceAll(">", "]")); // hudson parameters use <> instead of []
			sm_personSubscriptionPoolProductData	= new JSONArray(getProperty("sm.person.subscriptionPoolProductData", "[]").replaceFirst("^\"", "").replaceFirst("\"$", "").replaceAll("<", "[").replaceAll(">", "]")); // hudson JSONArray parameters get surrounded with double quotes that need to be stripped
			sm_contentIntegrationTestData			= new JSONArray(getProperty("sm.content.integrationTestData", "[]").replaceFirst("^\"", "").replaceFirst("\"$", "").replaceAll("<", "[").replaceAll(">", "]")); // hudson JSONArray parameters get surrounded with double quotes that need to be stripped

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
		if (property==null) return null;
		return (property.startsWith("$") || property.equals(""))? def:property;
	}

	
	
	public List<String> getPersonProductIds() throws JSONException {
		List<String> personProductIds = new ArrayList<String>();
		for (int j=0; j<sm_personSubscriptionPoolProductData.length(); j++) {
//			try {
				JSONObject poolProductDataAsJSONObject = (JSONObject) sm_personSubscriptionPoolProductData.get(j);
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
