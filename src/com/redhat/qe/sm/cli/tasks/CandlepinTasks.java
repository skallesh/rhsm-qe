package com.redhat.qe.sm.cli.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;

import com.redhat.qe.api.helper.TestHelper;
import com.redhat.qe.auto.selenium.Base64;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.auto.testng.LogMessageUtil;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.RevokedCert;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;
import com.redhat.qe.tools.SSLCertificateTruster;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


/**
 * @author jsefler
 *
 * Reference: Candlepin RESTful API Documentation: https://fedorahosted.org/candlepin/wiki/API
 */
public class CandlepinTasks {

	protected static Logger log = Logger.getLogger(SubscriptionManagerTasks.class.getName());
	protected /*NOT static*/ SSHCommandRunner sshCommandRunner = null;
	protected /*NOT static*/ String serverInstallDir = null;
	protected /*NOT static*/ String serverImportDir = null;
	public static String candlepinCRLFile	= "/var/lib/candlepin/candlepin-crl.crl";
	public static String defaultConfigFile	= "/etc/candlepin/candlepin.conf";
	public static String rubyClientDir	= "/client/ruby";
	public static File candlepinCACertFile = new File("/etc/candlepin/certs/candlepin-ca.crt");
	public static String generatedProductsDir	= "/proxy/generated_certs";
	public static HttpClient client;
	public boolean isOnPremises = false;
	public String branch = "";

	static {
		MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
      	client = new HttpClient(connectionManager);
      	client.getParams().setAuthenticationPreemptive(true);
		//client = new HttpClient();
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
	
	/**
	 * @param sshCommandRunner
	 * @param serverInstallDir
	 * @param serverImportDir
	 * @param isOnPremises
	 * @param branch - git branch (or tag) to deploy.  The most common values are "master" and "candlepin-latest-tag" (which is a special case)
	 */
	public CandlepinTasks(SSHCommandRunner sshCommandRunner, String serverInstallDir, String serverImportDir, boolean isOnPremises, String branch) {
		super();
		this.sshCommandRunner = sshCommandRunner;
		this.serverInstallDir = serverInstallDir;
		this.serverImportDir = serverImportDir;
		this.isOnPremises = isOnPremises;
		this.branch = branch;
	}
	
	
	public void deploy() {
		String hostname = sshCommandRunner.getConnection().getHostname();

		if (branch==null || branch.equals("")) {
			log.info("Skipping deploy of candlepin server since no branch was specified.");
			return;
		}
		
		log.info("Upgrading the server to the latest git tag...");
		Assert.assertEquals(RemoteFileTasks.testFileExists(sshCommandRunner, serverInstallDir),1,"Found the server install directory "+serverInstallDir);

		RemoteFileTasks.searchReplaceFile(sshCommandRunner, "/etc/sudoers", "\\(^Defaults[[:space:]]\\+requiretty\\)", "#\\1");	// Needed to prevent error:  sudo: sorry, you must have a tty to run sudo
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout master; git pull", Integer.valueOf(0), null, "(Already on|Switched to branch) 'master'");
		if (branch.equals("candlepin-latest-tag")) {  // see commented python code at the end of this file */
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git tag | grep candlepin-0.4 | sort -t . -k 3 -n | tail -1", Integer.valueOf(0), "^candlepin", null);
			branch = sshCommandRunner.getStdout().trim();
		}
		if (branch.startsWith("candlepin-")) {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout "+branch, Integer.valueOf(0), null, "HEAD is now at .* package \\[candlepin\\] release \\["+branch.substring(branch.indexOf("-")+1)+"\\]."); //HEAD is now at 560b098... Automatic commit of package [candlepin] release [0.0.26-1].
	
		} else {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout "+branch+"; git pull origin "+branch, Integer.valueOf(0), null, "(Already on|Switched to branch|Switched to a new branch) '"+branch+"'");	// Switched to branch 'master' // Already on 'master' // Switched to a new branch 'BETA'
		}
		if (!serverImportDir.equals("")) {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverImportDir+"; git pull", Integer.valueOf(0));
		}
		/* TODO: RE-INSTALL GEMS HELPS WHEN THERE ARE DEPLOY ERRORS	
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "for item in $(for gem in $(gem list | grep -v \"\\*\"); do echo $gem; done | grep -v \"(\" | grep -v \")\"); do echo 'Y' | gem uninstall $item -a; done", Integer.valueOf(0), "Successfully uninstalled", null);	// probably only needs to be run once  // for item in $(for gem in $(gem list | grep -v "\*"); do echo $gem; done | grep -v "(" | grep -v ")"); do echo 'Y' | gem uninstall $item -a; done
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; gem install bundler", Integer.valueOf(0), "installed", null);	// probably only needs to be run once
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; gem install buildr", Integer.valueOf(0), "1 gem installed", null);	// probably only needs to be run once
		*/
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"/proxy; bundle install", Integer.valueOf(0), "Your bundle is complete!", null);	// Your bundle is complete! Use `bundle show [gemname]` to see where a bundled gem is installed.
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "service ntpd stop; ntpdate clock.redhat.com; service ntpd start; chkconfig ntpd on", /*Integer.valueOf(0) DON"T CHECK EXIT CODE SINCE IT RETURNS 1 WHEN STOP FAILS EVEN THOUGH START SUCCEEDS*/null, "Starting ntpd(.*?):\\s+\\[  OK  \\]", null);	// Starting ntpd:  [  OK  ]		// Starting ntpd (via systemctl):  [  OK  ]
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "service postgresql stop; service postgresql start", /*Integer.valueOf(0) DON"T CHECK EXIT CODE SINCE IT RETURNS 1 WHEN STOP FAILS EVEN THOUGH START SUCCEEDS*/null, "Starting postgresql(.*?):\\s+\\[  OK  \\]", null);	// Starting postgresql service: [  OK  ]	// Starting postgresql (via systemctl):  [  OK  ]
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "export TESTDATA=1; export FORCECERT=1; export GENDB=1; export HOSTNAME="+hostname+"; export IMPORTDIR="+serverImportDir+"; cd "+serverInstallDir+"/proxy; buildconf/scripts/deploy", Integer.valueOf(0), "Initialized!", null);
		// Update 1/21/2011                                    ^^^^^^ TESTDATA is new for master branch                                             ^^^^^^ IMPORTDIR applies to branches <= BETA

		/* attempt to use live logging
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait("cd "+serverInstallDir+"/proxy; buildconf/scripts/deploy", true);
			Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0));
			Assert.assertContainsMatch(sshCommandResult.getStdout(), "Initialized!");
		*/
		
		/* Note: if getting error on install from master branch:
/usr/lib/ruby/site_ruby/1.8/rubygems/custom_require.rb:31:in `gem_original_require': no such file to load -- oauth (LoadError)
	from /usr/lib/ruby/site_ruby/1.8/rubygems/custom_require.rb:31:in `require'
	from ../client/ruby/candlepin_api.rb:9
	from buildconf/scripts/import_products.rb:3:in `require'
	from buildconf/scripts/import_products.rb:3

		 * 
		 * Solution:
		 * # gem install oauth
		 */
		
		/* Note: if getting error on install from branch:
ssh root@mgmt5.rhq.lab.eng.bos.redhat.com export TESTDATA=1; export FORCECERT=1; export GENDB=1; export HOSTNAME=mgmt5.rhq.lab.eng.bos.redhat.com; export IMPORTDIR=/root/cp_product_utils; cd /root/candlepin/proxy; buildconf/scripts/deploy (com.redhat.qe.tools.SSHCommandRunner.run)
201105121112:39.195 - FINE: Stdout: 
Stopping tomcat6: [  OK  ]
using NO logdriver
============ generating a new db ==============
schema generation failed
 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
201105121112:39.196 - FINE: Stderr: 
/usr/lib/ruby/site_ruby/1.8/rubygems.rb:233:in `activate': can't activate rspec (= 2.1.0, runtime) for ["buildr-1.4.5"], already activated rspec-1.3.1 for [] (Gem::LoadError)
	from /usr/lib/ruby/site_ruby/1.8/rubygems.rb:249:in `activate'
	from /usr/lib/ruby/site_ruby/1.8/rubygems.rb:248:in `each'
	from /usr/lib/ruby/site_ruby/1.8/rubygems.rb:248:in `activate'
	from /usr/lib/ruby/site_ruby/1.8/rubygems.rb:1082:in `gem'
	from /usr/bin/buildr:18
/usr/lib/ruby/site_ruby/1.8/rubygems.rb:233:in `activate': can't activate rspec (= 2.1.0, runtime) for ["buildr-1.4.5"], already activated rspec-1.3.1 for [] (Gem::LoadError)
	from /usr/lib/ruby/site_ruby/1.8/rubygems.rb:249:in `activate'
	from /usr/lib/ruby/site_ruby/1.8/rubygems.rb:248:in `each'
	from /usr/lib/ruby/site_ruby/1.8/rubygems.rb:248:in `activate'
	from /usr/lib/ruby/site_ruby/1.8/rubygems.rb:1082:in `gem'
	from /usr/bin/buildr:18
		 * 
		 * 
		 * Solution: remove all gems...
		 * # for item in $(for gem in $(gem list | grep -v "\*"); do echo $gem; done | grep -v "("); do echo 'Y' | gem uninstall $item -a; done
		 * 
		 * Then install fresh...
		 * # gem install bundler
		 * # gem install buildr
		 * # bundle install  (in the proxy dir)
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
	
	static public String getResourceUsingRESTfulAPI(String server, String port, String prefix, String authenticator, String password, String path) throws Exception {
		GetMethod get = new GetMethod("https://"+server+":"+port+prefix+path);
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=684350 - jsefler 03/29/2011
		if (server.equals("subscription.rhn.stage.redhat.com")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="684350"; 
			try {if (invokeWorkaroundWhileBugIsOpen/*&&BzChecker.getInstance().isBugOpen(bugId)*/) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				/* THIS WORKAROUND CAME FROM Brenton AND IS TEMPRARY AGAINST STAGE ENV.
				 * stage:
				 *  curl -k -u stage_test_6:redhat --request GET http://rubyvip.web.stage.ext.phx2.redhat.com/clonepin/candlepin/entitlements/8a99f9812eddbd5c012f0343c0576c99
				 * webqa:
				 *  curl -k -u foo:bar --request GET http://rubyvip.web.qa.ext.phx1.redhat.com/clonepin/candlepin/status
				 */
				server = "rubyvip.web.stage.ext.phx2.redhat.com";
				port = "80";
				prefix = "/clonepin/candlepin";
				get = new GetMethod("http://"+server+":"+port+prefix+path);
			}
		}
		// END OF WORKAROUND
				
		String credentials = authenticator.equals("")? "":"-u "+authenticator+":"+password;
		log.info("SSH alternative to HTTP request: curl -k "+credentials+" --request GET https://"+server+":"+port+prefix+path);
		return getHTTPResponseAsString(client, get, authenticator, password);
	}
	static public String putResourceUsingRESTfulAPI(String server, String port, String prefix, String authenticator, String password, String path) throws Exception {
		PutMethod put = new PutMethod("https://"+server+":"+port+prefix+path);
		String credentials = authenticator.equals("")? "":"-u "+authenticator+":"+password;
		log.info("SSH alternative to HTTP request: curl -k "+credentials+" --request PUT https://"+server+":"+port+prefix+path);
		return getHTTPResponseAsString(client, put, authenticator, password);
	}
	static public String postResourceUsingRESTfulAPI(String server, String port, String prefix, String authenticator, String password, String path, String requestBody) throws Exception {
		PostMethod post = new PostMethod("https://"+server+":"+port+prefix+path);
		if (requestBody != null) {
			post.setRequestEntity(new StringRequestEntity(requestBody, "application/json", null));
			post.addRequestHeader("accept", "application/json");
			post.addRequestHeader("content-type", "application/json");
		}
		String credentials = authenticator.equals("")? "":"--user "+authenticator+":"+password;
		String data = requestBody==null? "":"--data '"+requestBody+"'";
		String headers = "";
		for ( org.apache.commons.httpclient.Header header : post.getRequestHeaders()) headers+= "--header '"+header.toString().trim()+"' ";

		log.info("SSH alternative to HTTP request: curl -k --request POST "+credentials+" "+data+" "+headers+" https://"+server+":"+port+prefix+path);

		return getHTTPResponseAsString(client, post, authenticator, password);
	}
	
	static public JSONObject getEntitlementUsingRESTfulAPI(String server, String port, String prefix, String owner, String password, String dbid) throws Exception {
		return new JSONObject(getResourceUsingRESTfulAPI(server, port, prefix, owner, password, "/entitlements/"+dbid));
	}

//	static public JSONObject curl_hateoas_ref_ASJSONOBJECT(SSHCommandRunner runner, String server, String port, String prefix, String owner, String password, String ref) throws JSONException {
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
		HttpMethod m = doHTTPRequest(client, method, username, password);
		String response = m.getResponseBodyAsString();
		log.finer("HTTP server returned content: " + response);
		m.releaseConnection();
		
		// When testing against a Stage or Production server where we are not granted enough authority to make HTTP Requests,
		// our tests will fail.  This block of code is a short cut to simply skip those test. - jsefler 11/15/2010 
		if (m.getStatusText().equalsIgnoreCase("Unauthorized")) {
			throw new SkipException("Not authorized make HTTP request to '"+m.getURI()+"' with credentials: username='"+username+"' password='"+password+"'");
		}
		
		return response;
	}
	
	protected static InputStream getHTTPResponseAsStream(HttpClient client, HttpMethod method, String username, String password) 
	throws Exception {
		HttpMethod m =  doHTTPRequest(client, method, username, password);
		InputStream result = m.getResponseBodyAsStream();
		//m.releaseConnection();
		return result;
	}
	
	protected static HttpMethod doHTTPRequest(HttpClient client, HttpMethod method, String username, String password) 
	throws Exception {
		String server = method.getURI().getHost();
		int port = method.getURI().getPort();
	
		setCredentials(client, server, port, username, password);
		log.finer("Running HTTP request: " + method.getName() + " on " + method.getURI() + " with credentials for '"+username+"' on server '"+server+"'...");
		if (method instanceof PostMethod){
			RequestEntity entity =  ((PostMethod)method).getRequestEntity();
			log.finer("HTTP Request entity: " + ((StringRequestEntity)entity).getContent());
		}
		log.finer("HTTP Request Headers: " + TestHelper.interpose(", ", (Object[])method.getRequestHeaders()));
		int responseCode = client.executeMethod(method);
		log.finer("HTTP server returned: " + responseCode) ;
		return method;
	}
	
	protected static void setCredentials(HttpClient client, String server, int port, String username, String password) {
		if (!username.equals(""))
			client.getState().setCredentials(
	            new AuthScope(server, port, AuthScope.ANY_REALM),
	            new UsernamePasswordCredentials(username, password)
	        );
	}
	/**
	 * @param server
	 * @param port
	 * @param prefix
	 * @param owner
	 * @param password
	 * @return a JSONObject representing the jobDetail.  Example:<br>
	 * 	{
	 * 	  "id" : "refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d",
	 * 	  "state" : "FINISHED",
	 * 	  "result" : "Pools refreshed for owner admin",
	 * 	  "startTime" : "2010-08-30T20:01:11.724+0000",
	 * 	  "finishTime" : "2010-08-30T20:01:11.800+0000",
	 * 	  "statusPath" : "/jobs/refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d",
	 * 	  "updated" : "2010-08-30T20:01:11.932+0000",
	 * 	  "created" : "2010-08-30T20:01:11.721+0000"
	 * 	}
	 * @throws Exception
	 */
	static public JSONObject refreshPoolsUsingRESTfulAPI(String server, String port, String prefix, String user, String password, String owner) throws Exception {
//		PutMethod put = new PutMethod("https://"+server+":"+port+prefix+"/owners/"+owner+"/subscriptions");
//		String response = getHTTPResponseAsString(client, put, owner, password);
//				
//		return new JSONObject(response);
		return new JSONObject(putResourceUsingRESTfulAPI(server, port, prefix, user, password, "/owners/"+owner+"/subscriptions"));
	}
	
	static public void exportConsumerUsingRESTfulAPI(String server, String port, String prefix, String owner, String password, String consumerKey, String intoExportZipFile) throws Exception {
		log.info("Exporting the consumer '"+consumerKey+"' for owner '"+owner+"' on candlepin server '"+server+"'...");
		log.info("SSH alternative to HTTP request: curl -k -u "+owner+":"+password+" https://"+server+":"+port+prefix+"/consumers/"+consumerKey+"/export > "+intoExportZipFile);
		// CURL EXAMPLE: /usr/bin/curl -k -u admin:admin https://jsefler-f12-candlepin.usersys.redhat.com:8443/candlepin/consumers/0283ba29-1d48-40ab-941f-2d5d2d8b222d/export > /tmp/export.zip
	
		boolean validzip = false;
		GetMethod get = new GetMethod("https://"+server+":"+port+prefix+"/consumers/"+consumerKey+"/export");
		InputStream response = getHTTPResponseAsStream(client, get, owner, password);
		File zipFile = new File(intoExportZipFile);
		FileOutputStream fos = new FileOutputStream(zipFile);

		try {
			//ZipInputStream zip = new ZipInputStream(response);
			//ZipEntry ze = zip.getNextEntry();
			byte[] buffer = new byte[1024];
			int len;
			while ((len = response.read(buffer)) != -1) {
			    fos.write(buffer, 0, len);
			}
			new ZipFile(zipFile);  //will throw exception if not valid zipfile
			validzip = true;
		}
		catch(Exception e) {
			log.log(Level.INFO, "Unable to read response as zip file.", e);
		}
		finally{
			get.releaseConnection();
			fos.flush();
			fos.close();
		}
		
		Assert.assertTrue(validzip, "Response is a valid zip file.");
	}
	
	static public void importConsumerUsingRESTfulAPI(String server, String port, String prefix, String owner, String password, String ownerKey, String fromExportZipFile) throws Exception {
		log.info("Importing consumer to owner '"+ownerKey+"' on candlepin server '"+server+"'...");
		//log.info("SSH alternative to HTTP request: curl -k -u "+owner+":"+password+" -F export=@"+fromExportZipFile+" https://"+server+":"+port+prefix+"/owners/"+ownerKey+"/import");
		log.info("SSH alternative to HTTP request: curl -k -u "+owner+":"+password+" -F export=@"+fromExportZipFile+" https://"+server+":"+port+prefix+"/owners/"+ownerKey+"/imports");
		// CURL EXAMPLE: curl -u admin:admin -k -F export=@/tmp/export.zip https://jsefler-f12-candlepin.usersys.redhat.com:8443/candlepin/owners/dopey/import

		//PostMethod post = new PostMethod("https://"+server+":"+port+prefix+"/owners/"+ownerKey+"/import");	// candlepin branch 0.2-
		PostMethod post = new PostMethod("https://"+server+":"+port+prefix+"/owners/"+ownerKey+"/imports");		// candlepin branch 0.3+ (/import changed to /imports)
		File f = new File(fromExportZipFile);
		Part[] parts = {
			      new FilePart(f.getName(), f)
			  };
		post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
		int status = client.executeMethod(post);
		
		Assert.assertEquals(status, 204);
	}
	
	/**
	 * @param server
	 * @param port
	 * @param prefix
	 * @param authenticator  - must have superAdmin privileges to get the jsonOwner; username:password for consumerid is not enough
	 * @param password
	 * @param consumerId
	 * @return
	 * @throws JSONException
	 * @throws Exception
	 */
	public static JSONObject getOwnerOfConsumerId(String server, String port, String prefix, String authenticator, String password, String consumerId) throws JSONException, Exception {
		// determine this consumerId's owner
		JSONObject jsonOwner = null;
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(server, port, prefix, authenticator, password,"/consumers/"+consumerId));	
		JSONObject jsonOwner_ = (JSONObject) jsonConsumer.getJSONObject("owner");
		// Warning: this authenticator, password needs to be superAdmin
		jsonOwner = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(server, port, prefix, authenticator, password,jsonOwner_.getString("href")));	

		return jsonOwner;
	}
	
	public static String getOwnerKeyOfConsumerId(String server, String port, String prefix, String authenticator, String password, String consumerId) throws JSONException, Exception {
		// determine this consumerId's owner
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(server, port, prefix, authenticator, password,"/consumers/"+consumerId));	
		JSONObject jsonOwner_ = (JSONObject) jsonConsumer.getJSONObject("owner");
		// jsonOwner_.getString("href") takes the form /owners/6239231 where 6239231 is the key
		File href = new File(jsonOwner_.getString("href")); // use a File to represent the path
		return href.getName();
	}
	
	/**
	 * @param server
	 * @param port
	 * @param prefix
	 * @param username
	 * @param password
	 * @param key - name of the key whose value you want to get (e.g. "displayName", "key", "id")
	 * @return - a list of all the key values corresponding to each of the orgs that this username belongs to
	 * @throws JSONException
	 * @throws Exception
	 */
	public static List<String> getOrgsKeyValueForUser(String server, String port, String prefix, String username, String password, String key) throws JSONException, Exception {

		List<String> values = new ArrayList<String>();
		JSONArray jsonUsersOrgs = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(server, port, prefix, username, password,"/users/"+username+"/owners"));	
		for (int j = 0; j < jsonUsersOrgs.length(); j++) {
			JSONObject jsonOrg = (JSONObject) jsonUsersOrgs.get(j);
			// {
			//    "contentPrefix": null, 
			//    "created": "2011-07-01T06:39:58.740+0000", 
			//    "displayName": "Snow White", 
			//    "href": "/owners/snowwhite", 
			//    "id": "8a90f8c630e46c7e0130e46ce114000a", 
			//    "key": "snowwhite", 
			//    "parentOwner": null, 
			//    "updated": "2011-07-01T06:39:58.740+0000", 
			//    "upstreamUuid": null
			// }
			values.add(jsonOrg.getString(key));
		}
		return values;
	}

	public static String getOrgDisplayNameForOrgKey(String server, String port, String prefix, String authenticator, String password, String orgKey) throws JSONException, Exception {
		JSONObject jsonOrg = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(server, port, prefix, authenticator, password,"/owners/"+orgKey));	
		return jsonOrg.getString("displayName");
	}
	
	public static String getOrgIdForOrgKey(String server, String port, String prefix, String authenticator, String password, String orgKey) throws JSONException, Exception {
		JSONObject jsonOrg = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(server, port, prefix, authenticator, password,"/owners/"+orgKey));	
		return jsonOrg.getString("id");
	}
	
	public static void dropAllConsumers(final String server, final String port, final String prefix, final String owner, final String password) throws Exception{
		JSONArray consumers = new JSONArray(getResourceUsingRESTfulAPI(server, port, prefix, owner, password, "consumers"));
		List<String> refs = new ArrayList<String>();
		for (int i=0;i<consumers.length();i++) {
			JSONObject o = consumers.getJSONObject(i);
			refs.add(o.getString("href"));
		}
		final ExecutorService service = Executors.newFixedThreadPool(4);  //run 4 concurrent deletes
		for (final String consumer: refs) {
			service.submit(new Runnable() {
				public void run() {
					try {
						HttpMethod m = new DeleteMethod("https://"+server+":"+port+prefix + consumer);
						doHTTPRequest(client, m, owner, password);
						m.releaseConnection();
					}catch (Exception e) {
						log.log(Level.SEVERE, "Could not delete consumer: " + consumer, e);
					}
				}
			});
		}
		
		service.shutdown();
		service.awaitTermination(6, TimeUnit.HOURS);
	}
	
	
	public static List<String> getPoolIdsForSubscriptionId(String server, String port, String prefix, String authenticator, String password, String ownerKey, String forSubscriptionId) throws JSONException, Exception{
		List<String> poolIds = new ArrayList<String>();
		/* Example jsonPool:
		  		{
			    "id": "8a90f8b42e398f7a012e399000780147",
			    "attributes": [
			      {
			        "name": "requires_consumer_type",
			        "value": "system",
			        "updated": "2011-02-18T16:17:42.008+0000",
			        "created": "2011-02-18T16:17:42.008+0000"
			      },
			      {
			        "name": "virt_limit",
			        "value": "0",
			        "updated": "2011-02-18T16:17:42.008+0000",
			        "created": "2011-02-18T16:17:42.008+0000"
			      },
			      {
			        "name": "virt_only",
			        "value": "true",
			        "updated": "2011-02-18T16:17:42.009+0000",
			        "created": "2011-02-18T16:17:42.009+0000"
			      }
			    ],
			    "owner": {
			      "href": "/owners/admin",
			      "id": "8a90f8b42e398f7a012e398f8d310005"
			    },
			    "providedProducts": [
			      {
			        "id": "8a90f8b42e398f7a012e39900079014b",
			        "productName": "Awesome OS Server Bits",
			        "productId": "37060",
			        "updated": "2011-02-18T16:17:42.009+0000",
			        "created": "2011-02-18T16:17:42.009+0000"
			      }
			    ],
			    "endDate": "2012-02-18T00:00:00.000+0000",
			    "startDate": "2011-02-18T00:00:00.000+0000",
			    "productName": "Awesome OS with up to 4 virtual guests",
			    "quantity": 20,
			    "contractNumber": "39",
			    "accountNumber": "12331131231",
			    "consumed": 0,
			    "subscriptionId": "8a90f8b42e398f7a012e398ff0ef0104",
			    "productId": "awesomeos-virt-4",
			    "sourceEntitlement": null,
			    "href": "/pools/8a90f8b42e398f7a012e399000780147",
			    "activeSubscription": true,
			    "restrictedToUsername": null,
			    "updated": "2011-02-18T16:17:42.008+0000",
			    "created": "2011-02-18T16:17:42.008+0000"
			  }
		*/
		JSONArray jsonPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(server,port,prefix,authenticator,password,"/owners/"+ownerKey+"/pools"));	
		for (int i = 0; i < jsonPools.length(); i++) {
			JSONObject jsonPool = (JSONObject) jsonPools.get(i);
			String poolId = jsonPool.getString("id");
			String subscriptionId = jsonPool.getString("subscriptionId");
			if (forSubscriptionId.equals(subscriptionId)) {
				poolIds.add(poolId);
			}
		}
		return poolIds;
	}
	
	public static String getSubscriptionIdForPoolId(String server, String port, String prefix, String authenticator, String password, String forPoolId) throws JSONException, Exception{
		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(server,port,prefix,authenticator,password,"/pools/"+forPoolId));
		return jsonPool.getString("subscriptionId");
	}
	
	public static String getPoolIdFromProductNameAndContractNumber(String server, String port, String prefix, String authenticator, String password, String ownerKey, String fromProductName, String fromContractNumber) throws JSONException, Exception{

		/* Example jsonPool:
		  		{
			    "id": "8a90f8b42e398f7a012e399000780147",
			    "attributes": [
			      {
			        "name": "requires_consumer_type",
			        "value": "system",
			        "updated": "2011-02-18T16:17:42.008+0000",
			        "created": "2011-02-18T16:17:42.008+0000"
			      },
			      {
			        "name": "virt_limit",
			        "value": "0",
			        "updated": "2011-02-18T16:17:42.008+0000",
			        "created": "2011-02-18T16:17:42.008+0000"
			      },
			      {
			        "name": "virt_only",
			        "value": "true",
			        "updated": "2011-02-18T16:17:42.009+0000",
			        "created": "2011-02-18T16:17:42.009+0000"
			      }
			    ],
			    "owner": {
			      "href": "/owners/admin",
			      "id": "8a90f8b42e398f7a012e398f8d310005"
			    },
			    "providedProducts": [
			      {
			        "id": "8a90f8b42e398f7a012e39900079014b",
			        "productName": "Awesome OS Server Bits",
			        "productId": "37060",
			        "updated": "2011-02-18T16:17:42.009+0000",
			        "created": "2011-02-18T16:17:42.009+0000"
			      }
			    ],
			    "endDate": "2012-02-18T00:00:00.000+0000",
			    "startDate": "2011-02-18T00:00:00.000+0000",
			    "productName": "Awesome OS with up to 4 virtual guests",
			    "quantity": 20,
			    "contractNumber": "39",
			    "accountNumber": "12331131231",
			    "consumed": 0,
			    "subscriptionId": "8a90f8b42e398f7a012e398ff0ef0104",
			    "productId": "awesomeos-virt-4",
			    "sourceEntitlement": null,
			    "href": "/pools/8a90f8b42e398f7a012e399000780147",
			    "activeSubscription": true,
			    "restrictedToUsername": null,
			    "updated": "2011-02-18T16:17:42.008+0000",
			    "created": "2011-02-18T16:17:42.008+0000"
			  }
		*/
		JSONArray jsonPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(server,port,prefix,authenticator,password,"/owners/"+ownerKey+"/pools"));	
		for (int i = 0; i < jsonPools.length(); i++) {
			JSONObject jsonPool = (JSONObject) jsonPools.get(i);
			String poolId = jsonPool.getString("id");
			String productName = jsonPool.getString("productName");
			String contractNumber = jsonPool.getString("contractNumber");
			if (productName.equals(fromProductName) && contractNumber.equals(fromContractNumber)) {
				return poolId;
			}
		}
		return null;
	}
	
	public static boolean isSubscriptionPoolVirtOnly (String server, String port, String prefix, String authenticator, String password, String poolId) throws JSONException, Exception {
		
		/* # curl -k -u testuser1:password --request GET https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/pools/8a90f8c6313e2a7801313e2bf39c0310 | python -mjson.tool
		{
		    "accountNumber": "12331131231", 
		    "activeSubscription": true, 
		    "attributes": [
		        {
		            "created": "2011-07-18T16:54:53.085+0000", 
		            "id": "8a90f8c6313e2a7801313e2bf39d0311", 
		            "name": "requires_consumer_type", 
		            "updated": "2011-07-18T16:54:53.085+0000", 
		            "value": "system"
		        }, 
		        {
		            "created": "2011-07-18T16:54:53.085+0000", 
		            "id": "8a90f8c6313e2a7801313e2bf39d0312", 
		            "name": "virt_limit", 
		            "updated": "2011-07-18T16:54:53.085+0000", 
		            "value": "0"
		        }, 
		        {
		            "created": "2011-07-18T16:54:53.085+0000", 
		            "id": "8a90f8c6313e2a7801313e2bf39d0313", 
		            "name": "virt_only", 
		            "updated": "2011-07-18T16:54:53.085+0000", 
		            "value": "true"
		        }
		    ], 
		    "consumed": 0, 
		    "contractNumber": "62", 
		    "created": "2011-07-18T16:54:53.084+0000", 
		    "endDate": "2012-07-17T00:00:00.000+0000", 
		    "href": "/pools/8a90f8c6313e2a7801313e2bf39c0310", 
		    "id": "8a90f8c6313e2a7801313e2bf39c0310", 
		    "owner": {
		        "displayName": "Admin Owner", 
		        "href": "/owners/admin", 
		        "id": "8a90f8c6313e2a7801313e2aef9c0006", 
		        "key": "admin"
		    }, 
		    "productAttributes": [
		        {
		            "created": "2011-07-18T16:54:53.085+0000", 
		            "id": "8a90f8c6313e2a7801313e2bf39d0314", 
		            "name": "virt_limit", 
		            "productId": "awesomeos-virt-4", 
		            "updated": "2011-07-18T16:54:53.085+0000", 
		            "value": "4"
		        }, 
		        {
		            "created": "2011-07-18T16:54:53.085+0000", 
		            "id": "8a90f8c6313e2a7801313e2bf39d0315", 
		            "name": "type", 
		            "productId": "awesomeos-virt-4", 
		            "updated": "2011-07-18T16:54:53.085+0000", 
		            "value": "MKT"
		        }, 
		        {
		            "created": "2011-07-18T16:54:53.085+0000", 
		            "id": "8a90f8c6313e2a7801313e2bf39d0316", 
		            "name": "arch", 
		            "productId": "awesomeos-virt-4", 
		            "updated": "2011-07-18T16:54:53.085+0000", 
		            "value": "ALL"
		        }, 
		        {
		            "created": "2011-07-18T16:54:53.085+0000", 
		            "id": "8a90f8c6313e2a7801313e2bf39d0317", 
		            "name": "version", 
		            "productId": "awesomeos-virt-4", 
		            "updated": "2011-07-18T16:54:53.085+0000", 
		            "value": "6.1"
		        }, 
		        {
		            "created": "2011-07-18T16:54:53.085+0000", 
		            "id": "8a90f8c6313e2a7801313e2bf39d0318", 
		            "name": "variant", 
		            "productId": "awesomeos-virt-4", 
		            "updated": "2011-07-18T16:54:53.085+0000", 
		            "value": "ALL"
		        }
		    ], 
		    "productId": "awesomeos-virt-4", 
		    "productName": "Awesome OS with up to 4 virtual guests", 
		    "providedProducts": [
		        {
		            "created": "2011-07-18T16:54:53.085+0000", 
		            "id": "8a90f8c6313e2a7801313e2bf39d0319", 
		            "productId": "37060", 
		            "productName": "Awesome OS Server Bits", 
		            "updated": "2011-07-18T16:54:53.085+0000"
		        }
		    ], 
		    "quantity": 20, 
		    "restrictedToUsername": null, 
		    "sourceEntitlement": null, 
		    "startDate": "2011-07-18T00:00:00.000+0000", 
		    "subscriptionId": "8a90f8c6313e2a7801313e2be1c0022e", 
		    "updated": "2011-07-18T16:54:53.084+0000"
		}
		*/
		
		Boolean virt_only = null;	// indicates that the pool does not specify virt_only attribute
		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(server,port,prefix,authenticator,password,"/pools/"+poolId));	
		JSONArray jsonAttributes = jsonPool.getJSONArray("attributes");
		// loop through the attributes of this pool looking for the "virt_only" attribute
		for (int j = 0; j < jsonAttributes.length(); j++) {
			JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
			String attributeName = jsonAttribute.getString("name");
			if (attributeName.equals("virt_only")) {
				virt_only = jsonAttribute.getBoolean("value");
				break;
			}
		}
		virt_only = virt_only==null? false : virt_only;	// the absense of a "virt_only" attribute implies virt_only=false
		return virt_only;
	}

	public static boolean isSubscriptionPoolMultiEntitlement (String server, String port, String prefix, String authenticator, String password, String poolId) throws JSONException, Exception {
		
		/* # curl -k -u testuser1:password --request GET https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/pools/8a90f8c6313e2a7801313e2c06f806ef | python -mjson.tool
		{
		    "accountNumber": "12331131231", 
		    "activeSubscription": true, 
		    "attributes": [], 
		    "consumed": 0, 
		    "contractNumber": "2", 
		    "created": "2011-07-18T16:54:58.040+0000", 
		    "endDate": "2012-07-17T00:00:00.000+0000", 
		    "href": "/pools/8a90f8c6313e2a7801313e2c06f806ef", 
		    "id": "8a90f8c6313e2a7801313e2c06f806ef", 
		    "owner": {
		        "displayName": "Admin Owner", 
		        "href": "/owners/admin", 
		        "id": "8a90f8c6313e2a7801313e2aef9c0006", 
		        "key": "admin"
		    }, 
		    "productAttributes": [
		        {
		            "created": "2011-07-18T16:54:58.040+0000", 
		            "id": "8a90f8c6313e2a7801313e2c06f806f0", 
		            "name": "multi-entitlement", 
		            "productId": "awesomeos-scalable-fs", 
		            "updated": "2011-07-18T16:54:58.040+0000", 
		            "value": "yes"
		        }, 
		        {
		            "created": "2011-07-18T16:54:58.040+0000", 
		            "id": "8a90f8c6313e2a7801313e2c06f806f1", 
		            "name": "type", 
		            "productId": "awesomeos-scalable-fs", 
		            "updated": "2011-07-18T16:54:58.040+0000", 
		            "value": "MKT"
		        }, 
		        {
		            "created": "2011-07-18T16:54:58.040+0000", 
		            "id": "8a90f8c6313e2a7801313e2c06f806f2", 
		            "name": "arch", 
		            "productId": "awesomeos-scalable-fs", 
		            "updated": "2011-07-18T16:54:58.040+0000", 
		            "value": "ALL"
		        }, 
		        {
		            "created": "2011-07-18T16:54:58.040+0000", 
		            "id": "8a90f8c6313e2a7801313e2c06f806f3", 
		            "name": "version", 
		            "productId": "awesomeos-scalable-fs", 
		            "updated": "2011-07-18T16:54:58.040+0000", 
		            "value": "1.0"
		        }, 
		        {
		            "created": "2011-07-18T16:54:58.040+0000", 
		            "id": "8a90f8c6313e2a7801313e2c06f806f4", 
		            "name": "variant", 
		            "productId": "awesomeos-scalable-fs", 
		            "updated": "2011-07-18T16:54:58.040+0000", 
		            "value": "ALL"
		        }
		    ], 
		    "productId": "awesomeos-scalable-fs", 
		    "productName": "Awesome OS Scalable Filesystem", 
		    "providedProducts": [
		        {
		            "created": "2011-07-18T16:54:58.040+0000", 
		            "id": "8a90f8c6313e2a7801313e2c06f906f5", 
		            "productId": "37090", 
		            "productName": "Awesome OS Scalable Filesystem Bits", 
		            "updated": "2011-07-18T16:54:58.040+0000"
		        }
		    ], 
		    "quantity": 5, 
		    "restrictedToUsername": null, 
		    "sourceEntitlement": null, 
		    "startDate": "2011-07-18T00:00:00.000+0000", 
		    "subscriptionId": "8a90f8c6313e2a7801313e2b3e07007e", 
		    "updated": "2011-07-18T16:54:58.040+0000"
		}
		*/
		
		Boolean multi_entitlement = null;	// indicates that the pool's product does NOT have the "multi-entitlement" attribute
		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(server,port,prefix,authenticator,password,"/pools/"+poolId));	
		JSONArray jsonProductAttributes = jsonPool.getJSONArray("productAttributes");
		// loop through the productAttributes of this pool looking for the "multi-entitlement" attribute
		for (int j = 0; j < jsonProductAttributes.length(); j++) {
			JSONObject jsonProductAttribute = (JSONObject) jsonProductAttributes.get(j);
			String productAttributeName = jsonProductAttribute.getString("name");
			if (productAttributeName.equals("multi-entitlement")) {
				//multi_entitlement = jsonProductAttribute.getBoolean("value");
				multi_entitlement = jsonProductAttribute.getString("value").equalsIgnoreCase("yes") || jsonProductAttribute.getString("value").equalsIgnoreCase("true") || jsonProductAttribute.getString("value").equals("1");
				break;
			}
		}
		multi_entitlement = multi_entitlement==null? false : multi_entitlement;	// the absense of a "multi-entitlement" productAttribute implies multi-entitlement=false
		return multi_entitlement;
	}
	
	

	
	/**
	 * @param server
	 * @param port
	 * @param prefix
	 * @param owner
	 * @param password
	 * @param jobDetail - JSONObject of a jobDetail. Example:<br>
	 * 	{
	 * 	  "id" : "refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d",
	 * 	  "state" : "RUNNING",
	 * 	  "result" : "Pools refreshed for owner admin",
	 * 	  "startTime" : "2010-08-30T20:01:11.724+0000",
	 * 	  "finishTime" : "2010-08-30T20:01:11.800+0000",
	 * 	  "statusPath" : "/jobs/refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d",
	 * 	  "updated" : "2010-08-30T20:01:11.932+0000",
	 * 	  "created" : "2010-08-30T20:01:11.721+0000"
	 * 	}
	 * @param state - valid states: "PENDING", "CREATED", "RUNNING", "FINISHED"
	 * @param retryMilliseconds - sleep time between attempts to get latest JobDetail
	 * @param timeoutMinutes - give up waiting
	 * @return
	 * @throws Exception
	 */
	static public JSONObject waitForJobDetailStateUsingRESTfulAPI(String server, String port, String prefix, String owner, String password, JSONObject jobDetail, String state, int retryMilliseconds, int timeoutMinutes) throws Exception {
		String statusPath = jobDetail.getString("statusPath");
		int t = 0;
		
		// pause for the sleep interval; get the updated job detail; while the job detail's state has not yet changed
		do {
			// pause for the sleep interval
			SubscriptionManagerCLITestScript.sleep(retryMilliseconds); t++;	
			
			// get the updated job detail
			jobDetail = new JSONObject(getResourceUsingRESTfulAPI(server,port,prefix,owner,password,statusPath));
		} while (!jobDetail.getString("state").equalsIgnoreCase(state) || (t*retryMilliseconds >= timeoutMinutes*60*1000));
		
		// assert that the state was achieved within the timeout
		Assert.assertFalse((t*retryMilliseconds >= timeoutMinutes*60*1000), "JobDetail '"+jobDetail.getString("id")+"' changed state to '"+state+"' within '"+t*retryMilliseconds+"' milliseconds (timeout="+timeoutMinutes+" min)");

		return jobDetail;
// TODO
//		public void getJobDetail(String id) {
//			// /usr/bin/curl -u admin:admin -k --header 'Content-type: application/json' --header 'Accept: application/json' --request GET https://jsefler-f12-candlepin.usersys.redhat.com:8443/candlepin/jobs/refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d
//			
//			{
//				  "id" : "refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d",
//				  "state" : "FINISHED",
//				  "result" : "Pools refreshed for owner admin",
//				  "startTime" : "2010-08-30T20:01:11.724+0000",
//				  "finishTime" : "2010-08-30T20:01:11.800+0000",
//				  "statusPath" : "/jobs/refresh_pools_2adc6dee-790f-438f-95b5-567f14dcd67d",
//				  "updated" : "2010-08-30T20:01:11.932+0000",
//				  "created" : "2010-08-30T20:01:11.721+0000"
//				}
//		}

	}
	
	public void restartTomcat() {
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service tomcat6 restart",Integer.valueOf(0),"^Starting tomcat6: +\\[  OK  \\]$",null);
	}
	
	public List<RevokedCert> getCurrentlyRevokedCerts() {
		sshCommandRunner.runCommandAndWaitWithoutLogging("openssl crl -noout -text -in "+candlepinCRLFile);
		String crls = sshCommandRunner.getStdout();
		return RevokedCert.parse(crls);
	}

// DELETEME
//	/**
//	 * @param fieldName
//	 * @param fieldValue
//	 * @param revokedCerts - usually getCurrentlyRevokedCerts()
//	 * @return - the RevokedCert from revokedCerts that has a matching field (if not found, null is returned)
//	 */
//	public RevokedCert findRevokedCertWithMatchingFieldFromList(String fieldName, Object fieldValue, List<RevokedCert> revokedCerts) {
//		
//		RevokedCert revokedCertWithMatchingField = null;
//		for (RevokedCert revokedCert : revokedCerts) {
//			try {
//				if (RevokedCert.class.getField(fieldName).get(revokedCert).equals(fieldValue)) {
//					revokedCertWithMatchingField = revokedCert;
//				}
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (NoSuchFieldException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return revokedCertWithMatchingField;
//	}
	
	
	public JSONObject createOwnerUsingCPC(String owner_name) throws JSONException {
		log.info("Using the ruby client to create_owner owner_name='"+owner_name+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc create_owner \"%s\"", serverInstallDir+rubyClientDir, owner_name);
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);

		return new JSONObject(sshCommandResult.getStdout().replaceAll("=>", ":"));
		
		// REMINDER: DateFormat used in JSON objects is...
		// protected static String simpleDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";	// "2010-09-01T15:45:12.068+0000"

	}
	
	static public JSONObject createOwnerUsingRESTfulAPI(String server, String port, String prefix, String owner, String password, String owner_name) throws Exception {
// NOT TESTED
		return new JSONObject(postResourceUsingRESTfulAPI(server, port, prefix, owner, password, "/owners", owner_name));
	}
	
	public SSHCommandResult deleteOwnerUsingCPC(String owner_name) {
		log.info("Using the ruby client to delete_owner owner_name='"+owner_name+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc delete_owner \"%s\"", serverInstallDir+rubyClientDir, owner_name);
		return RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
	}
	
	public JSONObject createProductUsingCPC(String id, String name) throws JSONException {
		log.info("Using the ruby client to create_product id='"+id+"' name='"+name+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc create_product \"%s\" \"%s\"", serverInstallDir+rubyClientDir, id, name);
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
		
		return new JSONObject(sshCommandResult.getStdout().replaceAll("=>", ":"));
	}

	public JSONObject createSubscriptionUsingCPC(String ownerKey, String productId) throws JSONException {
		log.info("Using the ruby client to create_subscription ownerKey='"+ownerKey+"' productId='"+productId+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc create_subscription \"%s\" \"%s\"", serverInstallDir+rubyClientDir, ownerKey, productId);
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
		
		return new JSONObject(sshCommandResult.getStdout().replaceAll("=>", ":"));
	}
	
	public JSONObject createPoolUsingCPC(String productId, String productName, String ownerId, String quantity) throws JSONException {
		log.info("Using the ruby client to create_pool productId='"+productId+"' productName='"+productName+"' ownerId='"+ownerId+"' quantity='"+quantity+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc create_pool \"%s\" \"%s\" \"%s\" \"%s\"", serverInstallDir+rubyClientDir, productId, productName, ownerId, quantity);
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
		
		return new JSONObject(sshCommandResult.getStdout().replaceAll("=>", ":"));
	}
	
	public SSHCommandResult deletePoolUsingCPC(String id) {
		log.info("Using the ruby client to delete_pool id='"+id+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc delete_pool \"%s\"", serverInstallDir+rubyClientDir, id);
		return RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
	}
	
	public SSHCommandResult deleteSubscriptionUsingCPC(String id) {
		log.info("Using the ruby client to delete_subscription id='"+id+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc delete_subscription \"%s\"", serverInstallDir+rubyClientDir, id);
		return RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
	}
	
	public JSONObject refreshPoolsUsingCPC(String ownerKey, boolean immediate) throws JSONException {
		log.info("Using the ruby client to refresh_pools ownerKey='"+ownerKey+"' immediate='"+immediate+"'...");

		// call the ruby client
		String command = String.format("cd %s; ./cpc refresh_pools \"%s\" %s", serverInstallDir+rubyClientDir, ownerKey, Boolean.toString(immediate));
		SSHCommandResult sshCommandResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
		
		return new JSONObject(sshCommandResult.getStdout().replaceAll("=>", ":"));
	}
	
//	public static SyndFeed getSyndFeedForOwner(String key, String candlepinHostname, String candlepinPort, String candlepinPrefix, String candlepinUsername, String candlepinPassword) throws IllegalArgumentException, IOException, FeedException {
//		return getSyndFeedFor("owners",key,candlepinHostname,candlepinPort,candlepinPrefix,candlepinUsername,candlepinPassword);
//	}
//	
//	public static SyndFeed getSyndFeedForConsumer(String key, String candlepinHostname, String candlepinPort, String candlepinPrefix, String candlepinUsername, String candlepinPassword) throws IllegalArgumentException, IOException, FeedException {
//		return getSyndFeedFor("consumers",key,candlepinHostname,candlepinPort,candlepinPrefix,candlepinUsername,candlepinPassword);
//	}
//	
//	public static SyndFeed getSyndFeed(String candlepinHostname, String candlepinPort, String candlepinPrefix, String candlepinUsername, String candlepinPassword) throws IllegalArgumentException, IOException, FeedException {
//		return getSyndFeedFor(null,null,candlepinHostname,candlepinPort,candlepinPrefix,candlepinUsername,candlepinPassword);
//	}
//	
//	protected static SyndFeed getSyndFeedFor(String ownerORconsumer, String key, String candlepinHostname, String candlepinPort, String candlepinPrefix, String candlepinUsername, String candlepinPassword) throws IOException, IllegalArgumentException, FeedException {
//			
//		/* References:
//		 * http://www.exampledepot.com/egs/javax.net.ssl/TrustAll.html
//		 * http://www.avajava.com/tutorials/lessons/how-do-i-connect-to-a-url-using-basic-authentication.html
//		 * http://wiki.java.net/bin/view/Javawsxml/Rome
//		 */
//			
//		// Notes: Alternative curl approach to getting the atom feed:
//		// [ajay@garuda-rh proxy{pool_refresh}]$ curl -k -u admin:admin --request GET "https://localhost:8443/candlepin/owners/admin/atom" > /tmp/atom.xml; xmllint --format /tmp/atom.xml > /tmp/atom1.xml
//		// from https://bugzilla.redhat.com/show_bug.cgi?id=645597
//		
//		SSLCertificateTruster.trustAllCerts();
//		
//		// set the atom feed url for an owner, consumer, or null
//		String url = String.format("https://%s:%s%s/atom", candlepinHostname, candlepinPort, candlepinPrefix);
//		if (ownerORconsumer!=null && key!=null) {
//			url = String.format("https://%s:%s%s/%s/%s/atom", candlepinHostname, candlepinPort, candlepinPrefix, ownerORconsumer, key);
//		}
//		
//        log.fine("SyndFeedUrl: "+url);
//        String authString = candlepinUsername+":"+candlepinPassword;
//        log.finer("SyndFeedAuthenticationString: "+authString);
// 		byte[] authEncBytes = Base64.encodeBytesToBytes(authString.getBytes());
// 		String authStringEnc = new String(authEncBytes);
// 		log.finer("SyndFeed Base64 encoded SyndFeedAuthenticationString: "+authStringEnc);
//
// 		SyndFeed feed = null;
//        URL feedUrl=null;
//        URLConnection urlConnection=null;
////		try {
//			feedUrl = new URL(url);
//			urlConnection = feedUrl.openConnection();
//            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
//            SyndFeedInput input = new SyndFeedInput();
//            XmlReader xmlReader = new XmlReader(urlConnection);
//			feed = input.build(xmlReader);
//
////		} catch (MalformedURLException e1) {
////			// TODO Auto-generated catch block
////			e1.printStackTrace();
////		} catch (IOException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		} catch (IllegalArgumentException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		} catch (FeedException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
//			
//		// debug logging
//		log.finest("SyndFeed from "+feedUrl+":\n"+feed);
////log.fine("SyndFeed from "+feedUrl+":\n"+feed);
//		if (feed.getEntries().size()==0) {
//			log.fine(String.format("%s entries[] is empty", feed.getTitle()));		
//		} else for (int i=0;  i<feed.getEntries().size(); i++) {
//			log.fine(String.format("%s entries[%d].title=%s   description=%s", feed.getTitle(), i, ((SyndEntryImpl) feed.getEntries().get(i)).getTitle(), ((SyndEntryImpl) feed.getEntries().get(i)).getDescription()==null?"null":((SyndEntryImpl) feed.getEntries().get(i)).getDescription().getValue()));
//		}
//
//
//        return feed;
//	}
	public static SyndFeed getSyndFeedForOwner(String org, String candlepinHostname, String candlepinPort, String candlepinPrefix, String candlepinUsername, String candlepinPassword) throws IllegalArgumentException, IOException, FeedException {
		return getSyndFeedFor("/owners/"+org,candlepinHostname,candlepinPort,candlepinPrefix,candlepinUsername,candlepinPassword);
	}
	
	public static SyndFeed getSyndFeedForConsumer(String org, String uuid, String candlepinHostname, String candlepinPort, String candlepinPrefix, String candlepinUsername, String candlepinPassword) throws IllegalArgumentException, IOException, FeedException {
		return getSyndFeedFor("/owners/"+org+"/consumers/"+uuid,candlepinHostname,candlepinPort,candlepinPrefix,candlepinUsername,candlepinPassword);
	}
	
	public static SyndFeed getSyndFeed(String candlepinHostname, String candlepinPort, String candlepinPrefix, String candlepinUsername, String candlepinPassword) throws IllegalArgumentException, IOException, FeedException {
		return getSyndFeedFor("",candlepinHostname,candlepinPort,candlepinPrefix,candlepinUsername,candlepinPassword);
	}
	
	protected static SyndFeed getSyndFeedFor(String path, String candlepinHostname, String candlepinPort, String candlepinPrefix, String candlepinUsername, String candlepinPassword) throws IOException, IllegalArgumentException, FeedException {
			
		/* References:
		 * http://www.exampledepot.com/egs/javax.net.ssl/TrustAll.html
		 * http://www.avajava.com/tutorials/lessons/how-do-i-connect-to-a-url-using-basic-authentication.html
		 * http://wiki.java.net/bin/view/Javawsxml/Rome
		 */
			
		// Notes: Alternative curl approach to getting the atom feed:
		// [ajay@garuda-rh proxy{pool_refresh}]$ curl -k -u admin:admin --request GET "https://localhost:8443/candlepin/owners/admin/atom" > /tmp/atom.xml; xmllint --format /tmp/atom.xml > /tmp/atom1.xml
		// from https://bugzilla.redhat.com/show_bug.cgi?id=645597
		
		SSLCertificateTruster.trustAllCerts();
		
		// set the atom feed url for an owner, consumer, or null
		String url = String.format("https://%s:%s%s%s/atom", candlepinHostname, candlepinPort, candlepinPrefix, path);
//		if (ownerORconsumer!=null && key!=null) {
//			url = String.format("https://%s:%s%s/%s/%s/atom", candlepinHostname, candlepinPort, candlepinPrefix, ownerORconsumer, key);
//		}
		
        log.fine("SyndFeedUrl: "+url);
        String authString = candlepinUsername+":"+candlepinPassword;
        log.finer("SyndFeedAuthenticationString: "+authString);
 		byte[] authEncBytes = Base64.encodeBytesToBytes(authString.getBytes());
 		String authStringEnc = new String(authEncBytes);
 		log.finer("SyndFeed Base64 encoded SyndFeedAuthenticationString: "+authStringEnc);

 		SyndFeed feed = null;
        URL feedUrl=null;
        URLConnection urlConnection=null;

		feedUrl = new URL(url);
		urlConnection = feedUrl.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        SyndFeedInput input = new SyndFeedInput();
        XmlReader xmlReader = new XmlReader(urlConnection);
		feed = input.build(xmlReader);
			
		// debug logging
		log.finest("SyndFeed from "+feedUrl+":\n"+feed);
//log.fine("SyndFeed from "+feedUrl+":\n"+feed);
		if (feed.getEntries().size()==0) {
			log.fine(String.format("%s entries[] is empty", feed.getTitle()));		
		} else for (int i=0;  i<feed.getEntries().size(); i++) {
			log.fine(String.format("%s entries[%d].title=%s   description=%s", feed.getTitle(), i, ((SyndEntryImpl) feed.getEntries().get(i)).getTitle(), ((SyndEntryImpl) feed.getEntries().get(i)).getDescription()==null?"null":((SyndEntryImpl) feed.getEntries().get(i)).getDescription().getValue()));
		}

        return feed;
	}
		
	public static JSONObject createSubscriptionRequestBody(Integer quantity, Date startDate, Date endDate, String product, Integer contractNumber, Integer accountNumber, String... providedProducts) throws JSONException{
		JSONObject sub = new JSONObject();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		sub.put("startDate", sdf.format(startDate));
		sub.put("contractNumber", contractNumber);
		sub.put("accountNumber", accountNumber);
		sub.put("endDate", sdf.format(endDate));
		sub.put("quantity", quantity);

		List<JSONObject> pprods = new ArrayList<JSONObject>();
		for (String id: providedProducts) {
			JSONObject jo = new JSONObject();
			jo.put("id", id);
			pprods.add(jo);
		}
		sub.put("providedProducts", pprods);

		JSONObject prod = new JSONObject();
		prod.put("id", product);
		
		sub.put("product", prod);

		return sub;
	}
	
	public String invalidCredentialsRegexMsg() {
		return isOnPremises? "^Invalid Credentials$":"Invalid username or password. To create a login, please visit https://www.redhat.com/wapps/ugc/register.html";
	}
	
	public static void main (String... args) throws Exception {
		

		//System.out.println(CandlepinTasks.getResourceREST("candlepin1.devlab.phx1.redhat.com", "443", "xeops", "redhat", ""));
		//CandlepinTasks.dropAllConsumers("localhost", "8443", "admin", "admin");
		//CandlepinTasks.dropAllConsumers("candlepin1.devlab.phx1.redhat.com", "443", "xeops", "redhat");
		//CandlepinTasks.exportConsumerUsingRESTfulAPI("jweiss.usersys.redhat.com", "8443", "/candlepin", "admin", "admin", "78cf3c59-24ec-4228-a039-1b554ea21319", "/tmp/myfile.zip");
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -1);
		Date yday = cal.getTime();
		cal.add(Calendar.DATE, 2);
		Date trow = cal.getTime();
		
		
		//sub.put("quantity", 5);
		
		
		JSONArray ja = new JSONArray(Arrays.asList(new String[] {"blah" }));
		
		//jo.put("john", ja);
		//System.out.println(jo.toString());
	}
}




/* A PYTHON SCRIPT FROM jmolet TO HELP FIND THE candlepin-latest-tag
#!/usr/bin/python

class TreeNode:
  def __init__(self, value, vertices=None):
    self.value = value
    if vertices:
      self.vertices = vertices
    else:
      self.vertices = list()

  def __eq__(self, other):
    return self.value == other

  def __gt__(self, other):
    return self.value > other

  def __lt__(self, other):
    return self.value < other

def paths(node, stack=[], pathlist=[]):
  """Produces a list of all root-to-leaf paths in a tree."""

  if node.vertices:
    node.vertices.sort()
    for new_node in node.vertices:
      stack.append(new_node)
      paths(new_node, stack, pathlist)
      stack.pop()
  else:
    pathlist.append([node.value for node in stack])
  return pathlist

versions="0.0.1 0.0.10 0.0.11 0.0.12 0.0.13 0.0.14 0.0.15 0.0.16 0.0.17 0.0.18 0.0.19 0.0.2 0.0.21 0.0.22 0.0.23 0.0.24 0.0.25 0.0.26 0.0.27 0.0.28 0.0.29 0.0.3 0.0.30 0.0.31 0.0.32 0.0.33 0.0.34 0.0.35 0.0.36 0.0.37 0.0.38 0.0.39 0.0.4 0.0.40 0.0.41 0.0.42 0.0.43 0.0.5 0.0.6 0.0.7 0.0.8 0.0.9 0.1.1 0.1.10 0.1.11 0.1.12 0.1.13 0.1.14 0.1.15 0.1.16 0.1.17 0.1.18 0.1.19 0.1.2 0.1.20 0.1.21 0.1.22 0.1.23 0.1.24 0.1.25 0.1.26 0.1.27 0.1.28 0.1.29 0.1.3 0.1.4 0.1.5 0.1.6 0.1.7 0.1.8 0.1.9"

versions = versions.split(" ")
topnode = TreeNode(None)

# build tree of version numbers
for version in versions:
  nums = [int(num) for num in version.split(".")]
  cur_node = topnode
  for num in nums:
    if num in cur_node.vertices:
      cur_node = cur_node.vertices[cur_node.vertices.index(num)]
      continue
    new_node = TreeNode(num)
    cur_node.vertices.append(new_node)
    cur_node = new_node

# do DFS on version number tree
for path in paths(topnode):
  print ".".join([str(elem) for elem in path])
*/