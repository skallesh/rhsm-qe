package com.redhat.qe.sm.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
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
	public static HttpClient client;

	static {
		client = new HttpClient();
		try {
			SSLCertificateTruster.trustAllCertsForApacheHttp();
		}catch(Exception e) {
			log.log(Level.SEVERE, "Failed to trust all certificates for Apache HTTP Client", e);
		}
	}
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
	
	static public String getResourceREST(String server, String port, String owner, String password, String path) throws Exception {
		GetMethod get = new GetMethod("https://"+server+":"+port+"/candlepin"+path);
		return getHTTPResponseAsString(client, get, owner, password);
	}
	

//	static public JSONObject curl_hateoas_ref_ASJSONOBJECT(SSHCommandRunner runner, String server, String port, String owner, String password, String ref) throws JSONException {
//		log.info("Running HATEOAS command for '"+owner+"' on candlepin server '"+server+"'...");
//
//		String command = "/usr/bin/curl -u "+owner+":"+password+" -k https://"+server+":"+port+"/candlepin/"+ref;
//		
//		// execute the command from the runner (could be *any* runner)
//		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(runner, command, 0);
//		
//		return new JSONObject(sshCommandResult.getStdout());
//	}
	
	protected static String getHTTPResponseAsString(HttpClient client, HttpMethod method, String username, String password) 
	throws Exception {
		String response = doHTTPRequest(client, method, username, password).getResponseBodyAsString();
		log.info("HTTP server returned content = " + response);
		return response;
	}
	
	protected static InputStream getHTTPResponseAsStream(HttpClient client, HttpMethod method, String username, String password) 
	throws Exception {
		return doHTTPRequest(client, method, username, password).getResponseBodyAsStream();
	}
	
	protected static HttpMethod doHTTPRequest(HttpClient client, HttpMethod method, String username, String password) 
	throws Exception {
		String server = method.getURI().getHost();
		int port = method.getURI().getPort();
	
		setCredentials(client, server, port, username, password);
		log.info("Running HTTP request for '"+username+"' on server '"+server+"'...");
	
		int responseCode = client.executeMethod(method);
		log.info("HTTP server returned " + responseCode) ;
		return method;
	}
	
	protected static void setCredentials(HttpClient client, String server, int port, String username, String password) {
		client.getState().setCredentials(
	            new AuthScope(server, port, null),
	            new UsernamePasswordCredentials(username, password)
	        );
	}
	static public JSONObject refreshPoolsREST(String server, String port, String owner, String password) throws Exception {
		PutMethod put = new PutMethod("https://"+server+":"+port+"/candlepin/owners/"+owner+"/subscriptions");
		String response = getHTTPResponseAsString(client, put, owner, password);
				
		return new JSONObject(response);
	}
	
	static public void exportConsumerREST(String server, String port, String owner, String password, String consumerKey, String intoExportZipFile) throws Exception {
		log.info("Exporting the consumer '"+consumerKey+"' for owner '"+owner+"' on candlepin server '"+server+"'...");
		
		boolean validzip = false;
		GetMethod get = new GetMethod("https://"+server+":"+port+"/candlepin/consumers/"+consumerKey+"/export");
		InputStream response = getHTTPResponseAsStream(client, get, owner, password);
		try {
			ZipInputStream zip = new ZipInputStream(response);
			ZipEntry ze = zip.getNextEntry();
			validzip = true;
		}
		catch(Exception e) {
			log.log(Level.FINE, "Unable to read response as zip file.", e);
		}
		
		Assert.assertTrue(validzip, "Response is a valid zip file.");
	}
	
	static public void importConsumerREST(String server, String port, String owner, String password, String ownerKey, String fromExportZipFile) throws Exception {
		log.info("Importing consumer to owner '"+ownerKey+"' on candlepin server '"+server+"'...");
		
		PostMethod post = new PostMethod("https://"+server+":"+port+"/candlepin/owners/"+ownerKey+"/import");
		File f = new File(fromExportZipFile);
		Part[] parts = {
			      new FilePart(f.getName(), f)
			  };
		post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
		int status = client.executeMethod(post);
		
		Assert.assertEquals(status, 200);
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
	
	public static SyndFeed getSyndFeedForOwner(String key, String candlepinHostname, String candlepinPort, String candlepinUsername, String candlepinPassword) throws IllegalArgumentException, IOException, FeedException {
		return getSyndFeedFor("owners",key,candlepinHostname,candlepinPort,candlepinUsername,candlepinPassword);
	}
	
	public static SyndFeed getSyndFeedForConsumer(String key, String candlepinHostname, String candlepinPort, String candlepinUsername, String candlepinPassword) throws IllegalArgumentException, IOException, FeedException {
		return getSyndFeedFor("consumers",key,candlepinHostname,candlepinPort,candlepinUsername,candlepinPassword);
	}
	
	public static SyndFeed getSyndFeed(String candlepinHostname, String candlepinPort, String candlepinUsername, String candlepinPassword) throws IllegalArgumentException, IOException, FeedException {
		return getSyndFeedFor(null,null,candlepinHostname,candlepinPort,candlepinUsername,candlepinPassword);
	}
	
	protected static SyndFeed getSyndFeedFor(String ownerORconsumer, String key, String candlepinHostname, String candlepinPort, String candlepinUsername, String candlepinPassword) throws IOException, IllegalArgumentException, FeedException {
			
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
//		try {
			feedUrl = new URL(url);
			urlConnection = feedUrl.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            SyndFeedInput input = new SyndFeedInput();
            XmlReader xmlReader = new XmlReader(urlConnection);
			feed = input.build(xmlReader);

//		} catch (MalformedURLException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (FeedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		log.finer("SyndFeed from "+feedUrl+":\n"+feed);
        
        return feed;
	}
	
	public static void deleteAllConsumers(URL url, String login, String password) {
		
	}
	
	public static void main (String... args) throws Exception {
		
		System.out.println(CandlepinTasks.getResourceREST("candlepin1.devlab.phx1.redhat.com", "443", "xeops", "redhat", "/"));
	}
}
