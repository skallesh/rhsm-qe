package com.redhat.qe.sm.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.redhat.qe.auto.selenium.Base64;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.data.RevokedCert;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;
import com.redhat.qe.tools.SSLCertificateTruster;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


/**
 * @author jsefler
 *
 */
public class CandlepinTasks {

	protected static Logger log = Logger.getLogger(SubscriptionManagerTasks.class.getName());
	protected /*NOT static*/ SSHCommandRunner sshCommandRunner = null;
	protected /*NOT static*/ String serverInstallDir = null;
	public static String candlepinCRLFile	= "/var/lib/candlepin/candlepin-crl.crl";
	public static String defaultConfigFile	= "/etc/candlepin/candlepin.conf";
	public static String rubyClientDir	= "/client/ruby/";

	public CandlepinTasks() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public CandlepinTasks(SSHCommandRunner sshCommandRunner, String serverInstallDir) {
		super();
		this.sshCommandRunner = sshCommandRunner;
		this.serverInstallDir = serverInstallDir;
	}
	
	
	/**
	 * @param serverImportDir
	 * @param branch - git branch (or tag) to deploy.  The most common values are "master" and "candlepin-latest-tag" (which is a special case)
	 */
	public void deploy(String serverImportDir, String branch) {

		log.info("Upgrading the server to the latest git tag...");
		Assert.assertEquals(RemoteFileTasks.testFileExists(sshCommandRunner, serverInstallDir),1,"Found the server install directory "+serverInstallDir);

		RemoteFileTasks.searchReplaceFile(sshCommandRunner, "/etc/sudoers", "\\(^Defaults[[:space:]]\\+requiretty\\)", "#\\1");	// Needed to prevent error:  sudo: sorry, you must have a tty to run sudo
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout master; git pull", Integer.valueOf(0), null, "(Already on|Switched to branch) 'master'");
		if (branch.equals("candlepin-latest-tag")) {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git tag | sort -t . -k 3 -n | tail -1", Integer.valueOf(0), "^candlepin", null);
			branch = sshCommandRunner.getStdout().trim();
		}
		if (branch.startsWith("candlepin-")) {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout "+branch, Integer.valueOf(0), null, "HEAD is now at .* package \\[candlepin\\] release \\["+branch.substring(branch.indexOf("-")+1)+"\\]."); //HEAD is now at 560b098... Automatic commit of package [candlepin] release [0.0.26-1].
	
		} else {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout "+branch, Integer.valueOf(0), null, "(Already on|Switched to branch) '"+branch+"'");	// Switched to branch 'master' // Already on 'master'
		}
//		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout "+latestGitTag, Integer.valueOf(0), null, "HEAD is now at .* package \\[candlepin\\] release \\["+latestGitTag.substring(latestGitTag.indexOf("-")+1)+"\\]."); //HEAD is now at 560b098... Automatic commit of package [candlepin] release [0.0.26-1].
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "service postgresql restart", Integer.valueOf(0), "Starting postgresql service:\\s+\\[  OK  \\]", null);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "unset FORCECERT; export GENDB=true; export IMPORTDIR="+serverImportDir+"; cd "+serverInstallDir+"/proxy; buildconf/scripts/deploy", Integer.valueOf(0), "Initialized!", null);
		/* attempt to use live logging
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait("cd "+serverInstallDir+"/proxy; buildconf/scripts/deploy", true);
			Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0));
			Assert.assertContainsMatch(sshCommandResult.getStdout(), "Initialized!");
		*/
	}
	
	public void cleanOutCRL() {
		log.info("Cleaning out the certificate revocation list (CRL) "+candlepinCRLFile+"...");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "rm -f "+candlepinCRLFile, 0);
	}
	
	/**
	 * Note: Updating the candlepin server conf files requires a restart of the tomact server.
	 * @param parameter
	 * @param value
	 * 
	 */
	public void updateConfigFileParameter(String parameter, String value){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, defaultConfigFile, "^"+parameter+"\\s*=.*$", parameter+"="+value),
				0,"Updated candlepin config parameter '"+parameter+"' to value: " + value);
	}
	
	static public JSONObject curl_refresh_pools(SSHCommandRunner runner, String server, String port, String owner, String password) throws JSONException {
		log.info("Refreshing the subscription pools for owner '"+owner+"' on candlepin server '"+server+"'...");
		// /usr/bin/curl -u admin:admin -k --header 'Content-type: application/json' --header 'Accept: application/json' --request PUT https://localhost:8443/candlepin/owners/admin/subscriptions
		// /usr/bin/curl -u candlepin_system_admin:admin -k --header 'Content-type: application/json' --header 'Accept: application/json' --request PUT https://candlepin1.devlab.phx1.redhat.com:443/candlepin/owners/1616678/subscriptions                                                                                                                                                                                                           ^orgid of the user for whom you are refreshing pools can be found by connecting to the database -> see https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/entitlement_qe_get_products

		String command = "/usr/bin/curl -u "+owner+":"+password+" -k --header 'Content-type: application/json' --header 'Accept: application/json' --request PUT https://"+server+":"+port+"/candlepin/owners/"+owner+"/subscriptions";
		
		// execute the command from the runner (could be *any* runner)
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(runner, command, 0);
		
		return new JSONObject(sshCommandResult.getStdout());
	}
	
	static public void curl_export_consumer(SSHCommandRunner runner, String server, String port, String owner, String password, String consumerKey, String intoExportZipFile) throws JSONException {
		log.info("Exporting the consumer '"+consumerKey+"' for owner '"+owner+"' on candlepin server '"+server+"'...");
		// /usr/bin/curl -k -u admin:admin https://jsefler-f12-candlepin.usersys.redhat.com:8443/candlepin/consumers/0283ba29-1d48-40ab-941f-2d5d2d8b222d/export > /tmp/export.zip
		String command = "/usr/bin/curl -k -u "+owner+":"+password+" https://"+server+":"+port+"/candlepin/consumers/"+consumerKey+"/export > "+intoExportZipFile;
		
		// execute the command from the runner (could be *any* runner)
		RemoteFileTasks.runCommandAndAssert(runner, command, 0);
		RemoteFileTasks.runCommandAndAssert(runner, "unzip -t "+intoExportZipFile, 0);
	}
	
	static public void curl_import_consumer(SSHCommandRunner runner, String server, String port, String owner, String password, String ownerKey, String fromExportZipFile) throws JSONException {
		log.info("Importing consumer to owner '"+ownerKey+"' on candlepin server '"+server+"'...");
		// curl -u admin:admin -k -F export=@/tmp/export.zip https://jsefler-f12-candlepin.usersys.redhat.com:8443/candlepin/owners/dopey/import
		String command = "/usr/bin/curl -k -u "+owner+":"+password+" -F export=@"+fromExportZipFile+" https://"+server+":"+port+"/candlepin/owners/"+ownerKey+"/import";
		
		// execute the command from the runner (could be *any* runner)
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(runner, command, 0);
	}
	
	
	//TODO
//	public void getJobDetail(String id) {
//		// /usr/bin/curl -u admin:admin -k --header 'Content-type: application/json' --header 'Accept: application/json' --request GET https://jsefler-f12-candlepin.usersys.redhat.com:8443/candlepin/jobs/refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d
//		
//		{
//			  "id" : "refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d",
//			  "state" : "FINISHED",
//			  "result" : "Pools refreshed for owner admin",
//			  "startTime" : "2010-08-30T20:01:11.724+0000",
//			  "finishTime" : "2010-08-30T20:01:11.800+0000",
//			  "statusPath" : "/jobs/refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d",
//			  "updated" : "2010-08-30T20:01:11.932+0000",
//			  "created" : "2010-08-30T20:01:11.721+0000"
//			}
//	}
	
	public void restartTomcat() {
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service tomcat6 restart",Integer.valueOf(0),"^Starting tomcat6: +\\[  OK  \\]$",null);
	}
	
	public List<RevokedCert> getCurrentlyRevokedCerts() {
		sshCommandRunner.runCommandAndWait("openssl crl -noout -text -in "+candlepinCRLFile);
		String crls = sshCommandRunner.getStdout();
		return RevokedCert.parse(crls);
	}
	
	/**
	 * @param fieldName
	 * @param fieldValue
	 * @param revokedCerts - usually getCurrentlyRevokedCerts()
	 * @return - the RevokedCert from revokedCerts that has a matching field (if not found, null is returned)
	 */
	public RevokedCert findRevokedCertWithMatchingFieldFromList(String fieldName, Object fieldValue, List<RevokedCert> revokedCerts) {
		
		RevokedCert revokedCertWithMatchingField = null;
		for (RevokedCert revokedCert : revokedCerts) {
			try {
				if (RevokedCert.class.getField(fieldName).get(revokedCert).equals(fieldValue)) {
					revokedCertWithMatchingField = revokedCert;
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return revokedCertWithMatchingField;
	}
	
	
	public JSONObject cpc_create_owner(String owner_name) throws JSONException {
		log.info("Using the ruby client to create_owner owner_name='"+owner_name+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc create_owner \"%s\"", serverInstallDir+rubyClientDir, owner_name);
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);

		return new JSONObject(sshCommandResult.getStdout().replaceAll("=>", ":"));
		
		// REMINDER: DateFormat used in JSON objects is...
		// protected static String simpleDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";	// "2010-09-01T15:45:12.068+0000"

	}
	
	public SSHCommandResult cpc_delete_owner(String owner_name) {
		log.info("Using the ruby client to delete_owner owner_name='"+owner_name+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc delete_owner \"%s\"", serverInstallDir+rubyClientDir, owner_name);
		return RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
	}
	
	public JSONObject cpc_create_product(String id, String name) throws JSONException {
		log.info("Using the ruby client to create_product id='"+id+"' name='"+name+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc create_product \"%s\" \"%s\"", serverInstallDir+rubyClientDir, id, name);
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
		
		return new JSONObject(sshCommandResult.getStdout().replaceAll("=>", ":"));
	}

	public JSONObject cpc_create_pool(String productId, String ownerId, String quantity) throws JSONException {
		log.info("Using the ruby client to create_pool productId='"+productId+"' ownerId='"+ownerId+"' quantity='"+quantity+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc create_pool \"%s\" \"%s\" \"%s\"", serverInstallDir+rubyClientDir, productId, ownerId, quantity);
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
		
		return new JSONObject(sshCommandResult.getStdout().replaceAll("=>", ":"));
	}
	
	public SSHCommandResult cpc_delete_pool(String id) {
		log.info("Using the ruby client to delete_pool id='"+id+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc delete_pool \"%s\"", serverInstallDir+rubyClientDir, id);
		return RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
	}
	
	public JSONObject cpc_refresh_pools(String ownerKey, boolean immediate) throws JSONException {
		log.info("Using the ruby client to refresh_pools ownerKey='"+ownerKey+"' immediate='"+immediate+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc refresh_pools \"%s\" %s", serverInstallDir+rubyClientDir, ownerKey, Boolean.toString(immediate));
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
		
		return new JSONObject(sshCommandResult.getStdout().replaceAll("=>", ":"));
	}
	
	public static SyndFeed getSyndFeedForOwner(String key, String candlepinHostname, String candlepinPort, String candlepinUsername, String candlepinPassword) {
		return getSyndFeedFor("owners",key,candlepinHostname,candlepinPort,candlepinUsername,candlepinPassword);
	}
	
	public static SyndFeed getSyndFeedForConsumer(String key, String candlepinHostname, String candlepinPort, String candlepinUsername, String candlepinPassword) {
		return getSyndFeedFor("consumers",key,candlepinHostname,candlepinPort,candlepinUsername,candlepinPassword);
	}
	
	public static SyndFeed getSyndFeed(String candlepinHostname, String candlepinPort, String candlepinUsername, String candlepinPassword) {
		return getSyndFeedFor(null,null,candlepinHostname,candlepinPort,candlepinUsername,candlepinPassword);
	}
	
	protected static SyndFeed getSyndFeedFor(String ownerORconsumer, String key, String candlepinHostname, String candlepinPort, String candlepinUsername, String candlepinPassword) {
			
		/* References:
		 * http://www.exampledepot.com/egs/javax.net.ssl/TrustAll.html
		 * http://www.avajava.com/tutorials/lessons/how-do-i-connect-to-a-url-using-basic-authentication.html
		 * http://wiki.java.net/bin/view/Javawsxml/Rome
		 */
			
		SSLCertificateTruster.trustAllCerts();
		
		// set the atom feed url for an owner, consumer, or null
		String url = String.format("https://%s:%s/candlepin/atom", candlepinHostname, candlepinPort);
		if (ownerORconsumer!=null && key!=null) {
			url = String.format("https://%s:%s/candlepin/%s/%s/atom", candlepinHostname, candlepinPort, ownerORconsumer, key);
		}
		
        log.fine("SyndFeedUrl: "+url);
        String authString = candlepinUsername+":"+candlepinPassword;
        log.finer("SyndFeedAuthenticationString: "+authString);
 		byte[] authEncBytes = Base64.encodeBytesToBytes(authString.getBytes());
 		String authStringEnc = new String(authEncBytes);
 		log.finer("SyndFeed Base64 encoded SyndFeedAuthenticationString: "+authStringEnc);

 		SyndFeed feed = null;
        URL feedUrl=null;
        URLConnection urlConnection=null;
		try {
			feedUrl = new URL(url);
			urlConnection = feedUrl.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            SyndFeedInput input = new SyndFeedInput();
            XmlReader xmlReader = new XmlReader(urlConnection);
			feed = input.build(xmlReader);

		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FeedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.finer("SyndFeed from "+feedUrl+":\n"+feed);
        
        return feed;
	}
}
