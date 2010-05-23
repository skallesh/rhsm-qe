package com.redhat.qe.sm.tests;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SCPTools;
import com.redhat.qe.tools.SSHCommandRunner;

import com.redhat.qe.auto.testng.TestScript;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.tasks.EntitlementCert;
import com.redhat.qe.sm.tasks.Pool;
import com.redhat.qe.sm.tasks.ProductID;
import com.redhat.qe.tools.SSHCommandRunner;

public class Setup extends TestScript{
	protected static final String defaultAutomationPropertiesFile=System.getenv("HOME")+"/sm-tests.properties";
	public static final String RHSM_LOC = "/usr/sbin/subscription-manager-cli ";
	
	String clientHostname				= System.getProperty("rhsm.client.hostname");
	String serverHostname				= System.getProperty("rhsm.server.hostname");
	String username						= System.getProperty("rhsm.client.username");
	String password						= System.getProperty("rhsm.client.password");
	String tcUnacceptedUsername			= System.getProperty("rhsm.client.username.tcunaccepted", "ssalevan");
	String tcUnacceptedPassword			= System.getProperty("rhsm.client.password.tcunaccepted", "redhat");
	String regtoken						= System.getProperty("rhsm.client.regtoken");
	String certFrequency				= System.getProperty("rhsm.client.certfrequency");
	String rpmLocation					= System.getProperty("rhsm.rpm");
	String prodCertLocation				= System.getProperty("rhsm.prodcert", "http://gibson.usersys.redhat.com/automatjon/subscription-manager-gui/testcerts/product/rhds.pem");
	String prodCertProduct				= System.getProperty("rhsm.prodcert.product", "Red Hat Directory Server");
	String serverPort 					= System.getProperty("rhsm.server.port");
	String serverBaseUrl				= System.getProperty("rhsm.server.baseurl");
	String clientsshKeyPrivate			= System.getProperty("rhsm.sshkey.private",".ssh/id_auto_dsa");
	String clientsshUser				= System.getProperty("rhsm.ssh.user","root");
	String clientsshkeyPassphrase		= System.getProperty("rhsm.sshkey.passphrase","");
	
	String defaultConfigFile			= "/etc/rhsm/rhsm.conf";
	String rhsmcertdLogFile				= "/var/log/rhsm/rhsmcertd.log";
	String rhsmYumRepoFile				= "/etc/yum/pluginconf.d/rhsmplugin.conf";
	
	ArrayList<Pool> availPools = new ArrayList<Pool>();
	ArrayList<ProductID> consumedProductIDs = new ArrayList<ProductID>();
	ArrayList<EntitlementCert> currentCerts = new ArrayList<EntitlementCert>();
	
	public static SSHCommandRunner sshCommandRunner = null;

	public void refreshSubscriptions(){
		availPools.clear();
		consumedProductIDs.clear();
		currentCerts.clear();
		
		log.info("Refreshing subscription information...");
		
		//refresh available subscriptions
		sshCommandRunner.runCommandAndWait(RHSM_LOC + "list --available");
		String availOut = sshCommandRunner.getStdout();
		String availErr = sshCommandRunner.getStderr();
		Assert.assertFalse(availOut.toLowerCase().contains("traceback") |
				availErr.toLowerCase().contains("traceback"),
				"list --available call does not produce a traceback");
		
		ArrayList<HashMap<String, String>> poolList = this.parseAvailableEntitlements(availOut);
		for(HashMap<String, String> poolMap:poolList)
			availPools.add(new Pool(poolMap));
		
		
		/*
		String[] availSubs = availOut.split("\\n");
			
			//SSHCommandRunner.executeViaSSHWithReturn(clientHostname, "root",
				//RHSM_LOC + "list --available")
		
		//if extraneous output comes out over stdout, figure out where the useful output begins
		int outputBegin = 2;
		for(int i=0;i<availSubs.length;i++){
			if(availSubs[i].contains("productName"))
				break;
			outputBegin++;
		}
		
		for(int i=outputBegin;i<availSubs.length;i++)
			try {
				availPools.add(new Pool(availSubs[i].trim()));
			} catch (ParseException e) {
				e.printStackTrace();
				log.warning("Unparseable subscription line: "+ availSubs[i]);
			}
		*/
		
		//refresh consumed subscriptions
		sshCommandRunner.runCommandAndWait(RHSM_LOC + "list --consumed");
		String consumedOut = sshCommandRunner.getStdout();
		String consumedErr = sshCommandRunner.getStderr();
		Assert.assertFalse(consumedOut.toLowerCase().contains("traceback") |
				consumedErr.toLowerCase().contains("traceback"),
				"list --consumed call does not produce a traceback");
		
		ArrayList<HashMap<String, String>> productList = this.parseConsumedProducts(consumedOut);
		for(HashMap<String, String> productMap:productList)
			consumedProductIDs.add(new ProductID(productMap));
		
		/*
 		
 		String[] consumedSubs = consumedOut.split("\\n");
		//if extraneous output comes out over stdout, figure out where the useful output begins
		outputBegin = 2;
		for(int i=0;i<consumedSubs.length;i++){
			if(consumedSubs[i].contains("Product Consumed"))
				break;
			outputBegin++;
		}
		
		for(int i=outputBegin;i<consumedSubs.length;i++)
			try {
				consumedProductIDs.add(new ProductID(consumedSubs[i].trim()));
			} catch (ParseException e) {
				log.warning("Unparseable subscription line: "+ consumedSubs[i]);
			}
		*/
		//refresh entitlement certificates
		sshCommandRunner.runCommandAndWait(
			"find /etc/pki/entitlement/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
		);
		String certificates = sshCommandRunner.getStdout();
		HashMap<String,HashMap<String,String>> certMap = parseCerts(certificates);
		for(String certKey:certMap.keySet())
			currentCerts.add(new EntitlementCert(certKey, certMap.get(certKey)));
	}
	
	public boolean poolsNoLongerAvailable(ArrayList<Pool> beforeSubscription, ArrayList<Pool> afterSubscription){
		for(Pool beforePool:beforeSubscription)
			if (afterSubscription.contains(beforePool))
				return false;
		return true;
	}
	
	public void installLatestSMRPM(){
		log.info("Retrieving latest subscription-manager RPM...");
		sshCommandRunner.runCommandAndWait("rm -f /tmp/subscription-manager.rpm");
		sshCommandRunner.runCommandAndWait("wget -O /tmp/subscription-manager.rpm --no-check-certificate \""+rpmLocation+"\"");
		
		log.info("Installing newest subscription-manager RPM...");
		sshCommandRunner.runCommandAndWait("rpm --force -e subscription-manager subscription-manager-gnome");
		//using yumlocalinstall should enable testing on RHTS boxes right off the bat.
		sshCommandRunner.runCommandAndWait("yum -y localinstall /tmp/subscription-manager.rpm --nogpgcheck");
	}
	
	public void updateSMConfigFile(String hostname, String port){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, 
						defaultConfigFile, 
						"^hostname=.*$", 
						"hostname="+hostname),
						0,
						"Updated rhsm config hostname to point to:" + hostname
				);
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, 
						defaultConfigFile, 
						"^port=.*$", 
						"port="+port),
						0,
						"Updated rhsm config port to point to:" + port
				);
	}
	
	public void changeCertFrequency(String frequency){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, 
						defaultConfigFile, 
						"^certFrequency=.*$", 
						"certFrequency="+frequency),
						0,
						"Updated rhsmd cert refresh frequency to "+frequency+" minutes"
				);
		sshCommandRunner.runCommandAndWait("mv "+rhsmcertdLogFile+" "+rhsmcertdLogFile+".bak");
		sshCommandRunner.runCommandAndWait("service rhsmcertd restart");
		Assert.assertEquals(RemoteFileTasks.grepFile(sshCommandRunner,
				rhsmcertdLogFile,
				"started: interval = "+frequency),
				0,
				"interval reported as "+frequency+" in "+rhsmcertdLogFile);
	}
	
	public void registerToCandlepin(String username, String password){
		sshCommandRunner.runCommandAndWait(RHSM_LOC +
				"register --username="+username+" --password="+password + " --force");
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait(
						"stat /etc/pki/consumer/key.pem").intValue(),
						0,
						"/etc/pki/consumer/key.pem is present after register");
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait(
						"stat /etc/pki/consumer/cert.pem").intValue(),
						0,
						"/etc/pki/consumer/cert.pem is present after register");
	}
	
	public void registerToCandlepinAutosubscribe(String username, String password){
		sshCommandRunner.runCommandAndWait(RHSM_LOC +
				"register --username="+username+" --password="+password + " --force --autosubscribe");
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait(
						"stat /etc/pki/consumer/key.pem").intValue(),
						0,
						"/etc/pki/consumer/key.pem is present after register");
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait(
						"stat /etc/pki/consumer/cert.pem").intValue(),
						0,
						"/etc/pki/consumer/cert.pem is present after register");
	}
	
	public void checkInvalidRegistrationStrings(String username, String password){
		sshCommandRunner.runCommandAndWait(RHSM_LOC +
				"register --username="+username+this.getRandInt()+
				" --password="+password+this.getRandInt()+" --force");
		Assert.assertContainsMatch(sshCommandRunner.getStdout(),
				"Invalid username or password. To create a login, please visit https:\\/\\/www.redhat.com\\/wapps\\/ugc\\/register.html");
	}
	
	public void unregisterFromCandlepin(){
		sshCommandRunner.runCommandAndWait(RHSM_LOC + 
				"unregister");
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,
				"ls /etc/pki/entitlement/product | grep pem");
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,
				"stat /etc/pki/consumer/key.pem");
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,
				"stat /etc/pki/consumer/cert.pem");
	}
	
	public void subscribeToPool(Pool pool, boolean withPoolID){
		if(withPoolID){
			log.info("Subscribing to pool with pool ID:"+ pool.poolName);
			sshCommandRunner.runCommandAndWait(RHSM_LOC +
					"subscribe --pool="+pool.poolId);
		}
		else{
			log.info("Subscribing to pool with pool name:"+ pool.poolName);
			sshCommandRunner.runCommandAndWait(RHSM_LOC +
					"subscribe --product=\""+pool.productSku+"\"");
		}
		this.refreshSubscriptions();
		Assert.assertTrue(consumedProductIDs.size() > 0, "Successfully subscribed to pool with pool ID: "+
				pool.poolId + " and pool name: "+ pool.poolName);
	}
	
	public void subscribeToRegToken(String regtoken){
		log.info("Subscribing to registration token: "+ regtoken);
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,
				RHSM_LOC+"subscribe --regtoken="+regtoken);
		this.refreshSubscriptions();
		Assert.assertTrue((this.consumedProductIDs.size() > 0),
				"At least one entitlement consumed by regtoken subscription");
	}
	
	public HashMap<String, String[]> getPackagesCorrespondingToSubscribedRepos(){
		int min = 3;
		sshCommandRunner.runCommandAndWait("killall -9 yum");
		log.info("timeout of "+min+" minutes for next command");
		sshCommandRunner.runCommandAndWait("yum list available",Long.valueOf(min*60000));
		HashMap<String, String[]> pkgMap = new HashMap<String, String[]>();
		
		String[] packageLines = sshCommandRunner.getStdout().split("\\n");
		
		int pkglistBegin = 0;
		
		for(int i=0;i<packageLines.length;i++){
			pkglistBegin++;
			if(packageLines[i].contains("Available Packages"))
				break;
		}
		
		for(ProductID sub:this.consumedProductIDs){
			ArrayList<String> pkgList = new ArrayList<String>();
			for(int i=pkglistBegin;i<packageLines.length;i++){
				String[] splitLine = packageLines[i].split(" ");
				String pkgName = splitLine[0];
				String repoName = splitLine[splitLine.length - 1];
				if(repoName.toLowerCase().contains(sub.productId.toLowerCase()))
					pkgList.add(pkgName);
			}
			pkgMap.put(sub.productId, (String[])pkgList.toArray());
		}
		
		return pkgMap;
	}
	
	public void unsubscribeFromProductID(ProductID pid){
		log.info("Unsubscribing from productID:"+ pid.productId);
		sshCommandRunner.runCommandAndWait(RHSM_LOC +
				"unsubscribe --serial=\""+pid.serialNumber+"\"");
		this.refreshSubscriptions();
		Assert.assertFalse(consumedProductIDs.contains(pid),
				"Successfully unsubscribed from productID: "+ pid.productId);
	}
	
	public void cleanOutAllCerts(){
		log.info("Cleaning out certs from /etc/pki/consumer, /etc/pki/entitlement/, /etc/pki/entitlement/product, and /etc/pki/product/");
		
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/consumer/*");
		sshCommandRunner.runCommandAndWait("rm -rf /etc/pki/entitlement/*");
		sshCommandRunner.runCommandAndWait("rm -rf /etc/pki/entitlement/product/*");
		sshCommandRunner.runCommandAndWait("rm -rf /etc/pki/product/*");
	}
	
	public void sleep(long i) {
		log.info("Sleeping for "+i+" milliseconds...");
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			log.info("Sleep interrupted!");
		}
	}
	
	public int getRandInt(){
		Random gen = new Random();
		return gen.nextInt();
	}
	
	public ArrayList<String> getYumRepolist(){
		ArrayList<String> repos = new ArrayList<String>();
		sshCommandRunner.runCommandAndWait("killall -9 yum");
		
		sshCommandRunner.runCommandAndWait("yum repolist");
		String[] availRepos = sshCommandRunner.getStdout().split("\\n");
		
		int repolistStartLn = 0;
		int repolistEndLn = 0;
		
		for(int i=0;i<availRepos.length;i++)
			if (availRepos[i].contains("repo id"))
				repolistStartLn = i + 1;
			else if (availRepos[i].contains("repolist:"))
				repolistEndLn = i;
		
		for(int i=repolistStartLn;i<repolistEndLn;i++)
			repos.add(availRepos[i].split(" ")[0]);
		
		return repos;
	}
	
	public void adjustRHSMYumRepo(boolean enabled){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, 
						rhsmYumRepoFile, 
						"^enabled=.*$", 
						"enabled="+(enabled?'1':'0')),
						0,
						"Adjusted RHSM Yum Repo config file, enabled="+(enabled?'1':'0')
				);
	}
	
	public void unsubscribeFromAllProductIDs(){
		log.info("Unsubscribing from all productIDs...");
		this.refreshSubscriptions();
		ArrayList<ProductID> consumedProductIDbefore = (ArrayList<ProductID>)this.consumedProductIDs.clone();
		for(ProductID sub:consumedProductIDbefore)
			this.unsubscribeFromProductID(sub);
		Assert.assertEquals(this.consumedProductIDs.size(),
				0,
				"Asserting that all productIDs are now unsubscribed");
		log.info("Verifying that entitlement certificates are no longer present...");
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,
				"ls /etc/pki/entitlement/product/ | grep pem");
	}
	
	public void subscribeToAllPools(boolean withPoolID){
		log.info("Subscribing to all pools"+
				(withPoolID?" (using pool ID)...":"..."));
		this.refreshSubscriptions();
		ArrayList<Pool>availablePools = (ArrayList<Pool>)this.availPools.clone();
		for (Pool sub:availablePools)
			this.subscribeToPool(sub, withPoolID);
		Assert.assertTrue(this.poolsNoLongerAvailable(availablePools, this.availPools),
				"Pool quantities successfully decremented");
	}
	
	@BeforeSuite(groups={"sm_setup"},description="subscription manager set up",alwaysRun=true)
	public void setupSM() throws ParseException, IOException{
		sshCommandRunner = new SSHCommandRunner(clientHostname, 
				clientsshUser, clientsshKeyPrivate, clientsshkeyPassphrase, null);
		this.installLatestSMRPM();
		this.unregisterFromCandlepin();
		this.cleanOutAllCerts();
		this.updateSMConfigFile(serverHostname, serverPort);
		this.changeCertFrequency(certFrequency);
		sshCommandRunner.runCommandAndWait("killall -9 yum");
	}
	
	public HashMap<String, HashMap<String, String>> parseCerts(String certificates){
		HashMap<String, HashMap<String, String>> productMap = new HashMap<String, HashMap<String, String>>();
		HashMap<String, String> regexes = new HashMap<String, String>();
		
		regexes.put("name", "1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.1:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("label", "1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.2:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("phys_ent", "1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.3:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("flex_ent", "1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.4:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("vendor_id", "1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.5:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("download_url", "1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.6:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("enabled", "1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.8:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		
		for(String component:regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(component), Pattern.MULTILINE);
			this.addRegexMatchesToMap(pat, certificates, productMap, component);
		}
		
		return productMap;
	}
	
	public void runRHSMCallAsLang(String lang,String rhsmCall){
		sshCommandRunner.runCommandAndWait("export LANG="+lang+"; "+RHSM_LOC + rhsmCall);
	}
	
	public void setLanguage(String lang){
		sshCommandRunner.runCommandAndWait("export LANG="+lang);
	}
	
	public ArrayList<HashMap<String, String>> parseAvailableEntitlements(String entitlements){
		ArrayList<HashMap<String, String>> entitlementList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> regexes = new HashMap<String, String>();
		
		regexes.put("poolName", "Name:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("productSku", "Product SKU:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("poolId", "PoolId:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("quantity", "quantity:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("endDate", "Expires:\\s*([a-zA-Z0-9 ,:()]*)");
		
		for(String component:regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(component), Pattern.MULTILINE);
			this.addRegexMatchesToList(pat, entitlements, entitlementList, component);
		}
		
		return entitlementList;
	}
	
	public ArrayList<HashMap<String, String>> parseConsumedProducts(String products){
		ArrayList<HashMap<String, String>> productList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> regexes = new HashMap<String, String>();
		
		regexes.put("productId", "Name:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("serialNumber", "SerialNumber:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("isActive", "Active:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("startDate", "Begins:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("endDate", "Expires:\\s*([a-zA-Z0-9 ,:()]*)");
		
		for(String component:regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(component), Pattern.MULTILINE);
			this.addRegexMatchesToList(pat, products, productList, component);
		}
		
		return productList;
	}
	
	private boolean addRegexMatchesToList(Pattern regex,
			String to_parse, ArrayList<HashMap<String, String>> matchList, String sub_key){
		Matcher matcher = regex.matcher(to_parse);
		int currListElem=0;
		while (matcher.find()){
			if (matchList.size() < currListElem + 1)
				matchList.add(new HashMap<String, String>());
			HashMap<String, String> matchMap = matchList.get(currListElem);
			matchMap.put(sub_key, matcher.group(1).trim());
			matchList.set(currListElem, matchMap);
			currListElem++;
		}
		return true;
	}
			
	
	private boolean addRegexMatchesToMap(Pattern regex, 
			String to_parse, HashMap<String, HashMap<String, String>> matchMap, String sub_key){
        Matcher matcher = regex.matcher(to_parse);
        while (matcher.find()) {
            HashMap<String, String> singleCertMap = matchMap.get(matcher.group(1));
            if(singleCertMap == null){
            	HashMap<String, String> newBranch = new HashMap<String, String>();
            	singleCertMap = newBranch;
            }
            singleCertMap.put(sub_key, matcher.group(2));
            matchMap.put(matcher.group(1), singleCertMap);
        }
        return true;
	}
}
