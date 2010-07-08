package com.redhat.qe.sm.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandRunner;
import com.redhat.qe.sm.abstractions.EntitlementCert;
import com.redhat.qe.sm.abstractions.Pool;
import com.redhat.qe.sm.abstractions.ProductCert;
import com.redhat.qe.sm.abstractions.ProductID;

public class ModuleTasks {

	protected static Logger log = Logger.getLogger(ModuleTasks.class.getName());
	protected static SSHCommandRunner sshCommandRunner = null;
	
	protected ArrayList<Pool> availPools = new ArrayList<Pool>();
	protected ArrayList<ProductID> consumedProductIDs = new ArrayList<ProductID>();
	protected ArrayList<EntitlementCert> currentEntitlementCerts = new ArrayList<EntitlementCert>();
	protected ArrayList<ProductCert> productCerts = new ArrayList<ProductCert>();

	public ModuleTasks() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public void setSSHCommandRunner(SSHCommandRunner runner) {
		sshCommandRunner = runner;
	}

	public ArrayList<Pool> getAvailPools() {
		return availPools;
	}

	public ArrayList<ProductID> getConsumedProductIDs() {
		return consumedProductIDs;
	}

	public ArrayList<EntitlementCert> getCurrentEntitlementCerts() {
		return currentEntitlementCerts;
	}

	public ArrayList<ProductCert> getProductCerts() {
		return productCerts;
	}
	
	public void refreshSubscriptions(){
		availPools.clear();
		consumedProductIDs.clear();
		currentEntitlementCerts.clear();
		productCerts.clear();
		
		log.info("Refreshing subscription information...");
		
		//refresh available subscriptions
		sshCommandRunner.runCommandAndWait("subscription-manager-cli list --available");
		String availOut = sshCommandRunner.getStdout();
		String availErr = sshCommandRunner.getStderr();
		Assert.assertFalse(availOut.toLowerCase().contains("traceback") |
				availErr.toLowerCase().contains("traceback"),
				"list --available call does not produce a traceback");
		
		ArrayList<HashMap<String,String>> poolList = this.parseAvailableEntitlements(availOut);
		for(HashMap<String,String> poolMap:poolList)
			availPools.add(new Pool(poolMap));
		
		//refresh consumed subscriptions
		sshCommandRunner.runCommandAndWait("subscription-manager-cli list --consumed");
		String consumedOut = sshCommandRunner.getStdout();
		String consumedErr = sshCommandRunner.getStderr();
		Assert.assertFalse(consumedOut.toLowerCase().contains("traceback") |
				consumedErr.toLowerCase().contains("traceback"),
				"list --consumed call does not produce a traceback");
		
		ArrayList<HashMap<String,String>> productList = this.parseConsumedProducts(consumedOut);
		for(HashMap<String,String> productMap:productList)
			consumedProductIDs.add(new ProductID(productMap));
		
		//refresh product certs
		sshCommandRunner.runCommandAndWait("subscription-manager-cli list");
		String prodCertOut = sshCommandRunner.getStdout();
		String prodCertErr = sshCommandRunner.getStderr();
		Assert.assertFalse(prodCertOut.toLowerCase().contains("traceback") |
				prodCertErr.toLowerCase().contains("traceback"),
				"list call does not produce a trarceback");
		
		ArrayList<HashMap<String,String>> prodCertList = this.parseAvailableProductCerts(prodCertOut);
		for(HashMap<String,String> prodCertMap:prodCertList)
			productCerts.add(new ProductCert(prodCertMap));
		
		//refresh entitlement certificates
		sshCommandRunner.runCommandAndWait(
			"find /etc/pki/entitlement/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
		);
		String certificates = sshCommandRunner.getStdout();
		HashMap<String,HashMap<String,String>> certMap = parseCerts(certificates);
		for(String certKey:certMap.keySet())
			currentEntitlementCerts.add(new EntitlementCert(certKey, certMap.get(certKey)));
	}
	
	public HashMap<String,String[]> getPackagesCorrespondingToSubscribedRepos(){
		int min = 3;
		sshCommandRunner.runCommandAndWait("killall -9 yum");
		log.info("timeout of "+min+" minutes for next command");
		sshCommandRunner.runCommandAndWait("yum list available",Long.valueOf(min*60000));
		HashMap<String,String[]> pkgMap = new HashMap<String,String[]>();
		
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

	// register module tasks ************************************************************
	
	public void registerToCandlepin(String username, String password){
		// FIXME may want to make force an optional arg
		sshCommandRunner.runCommandAndWait("subscription-manager-cli register --username="+username+" --password="+password + " --force");
		// FIXME: may want to assert this output and save or return it.  - jsefler 7/8/2010
		// Stdout: 3f92221c-4b26-4e49-96af-b31abd7bd28c admin admin
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/key.pem").intValue(), 0,
						"/etc/pki/consumer/key.pem is present after register");
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/cert.pem").intValue(),0,
						"/etc/pki/consumer/cert.pem is present after register");
	}
	
	public void registerToCandlepinAutosubscribe(String username, String password){
		sshCommandRunner.runCommandAndWait("subscription-manager-cli register --username="+username+" --password="+password + " --force --autosubscribe");
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/key.pem").intValue(),0,
						"/etc/pki/consumer/key.pem is present after register");
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/cert.pem").intValue(),0,
						"/etc/pki/consumer/cert.pem is present after register");
	}
	
	
	// unregister module tasks ************************************************************

	public void unregisterFromCandlepin(){
		sshCommandRunner.runCommandAndWait("subscription-manager-cli unregister");
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,"ls /etc/pki/entitlement/product | grep pem");
//		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,"stat /etc/pki/consumer/key.pem");
//		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,"stat /etc/pki/consumer/cert.pem");
//		Assert.assertEquals(RemoteFileTasks.testFileExists(sshCommandRunner, "/etc/pki/consumer/key.pem"),0,"The identify key has been removed after unregistering.");
//		Assert.assertEquals(RemoteFileTasks.testFileExists(sshCommandRunner, "/etc/pki/consumer/cert.pem"),0,"The identify certificate has been removed after unregistering.");
		Assert.assertEquals(sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/cert.pem"),Integer.valueOf(1),"The identify certificate has been removed after unregistering.");
		Assert.assertEquals(sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/key.pem"),Integer.valueOf(1),"The identify key has been removed after unregistering.");
	}
	
	// list module tasks ************************************************************
	
	public void listConsumed() {
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,"subscription-manager-cli list --consumed");
	}
	
	public void listAvailable() {
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,"subscription-manager-cli list --available");
	}
	
	// subscribe module tasks ************************************************************

	public void subscribeToProduct(String product){
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,"subscription-manager-cli subscribe --product="+product);
	}
	
	public void subscribeToPool(Pool pool, boolean withPoolID){
		if(withPoolID){
			log.info("Subscribing to pool with pool ID:"+ pool.poolName);
			sshCommandRunner.runCommandAndWait("subscription-manager-cli subscribe --pool="+pool.poolId);
		}
		else{
			log.info("Subscribing to pool with pool name:"+ pool.poolName);
			sshCommandRunner.runCommandAndWait("subscription-manager-cli subscribe --product=\""+pool.productSku+"\"");
		}
		this.refreshSubscriptions();
		Assert.assertTrue(consumedProductIDs.size() > 0, "Successfully subscribed to pool with pool ID: "+ pool.poolId +" and pool name: "+ pool.poolName);
		//TODO: add in more thorough product subscription verification
		// first improvement is to assert that the count of consumedProductIDs is at least one greater than the count of consumedProductIDs before the new pool was subscribed to.
	}
	
	public void subscribeToRegToken(String regtoken){
		log.info("Subscribing to registration token: "+ regtoken);
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner, "subscription-manager-cli subscribe --regtoken="+regtoken);
		this.refreshSubscriptions();
		Assert.assertTrue((this.consumedProductIDs.size() > 0),
				"At least one entitlement consumed by regtoken subscription");
	}
	
	public void subscribeToAllPools(boolean withPoolID){
		withPoolID = true;
		log.info("Subscribing to all pools"+
				(withPoolID?" (using pool ID)...":"..."));
		this.refreshSubscriptions();
		ArrayList<Pool>availablePools = (ArrayList<Pool>)this.availPools.clone();
		for (Pool sub:availablePools)
			this.subscribeToPool(sub, withPoolID);
		Assert.assertTrue(this.poolsNoLongerAvailable(availablePools, this.availPools),
				"Pool quantities successfully decremented");
	}
	
	// unsubscribe module tasks ************************************************************

	public void unsubscribeFromAllProductIDs(){
		log.info("Unsubscribing from all productIDs...");
		this.refreshSubscriptions();
		ArrayList<ProductID> consumedProductIDbefore = (ArrayList<ProductID>)this.consumedProductIDs.clone();
		for(ProductID sub:consumedProductIDbefore)
			this.unsubscribeFromProductID(sub);
		Assert.assertEquals(this.consumedProductIDs.size(),0,
				"Asserting that all productIDs are now unsubscribed");
		log.info("Verifying that entitlement certificates are no longer present...");
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,
				"ls /etc/pki/entitlement/product/ | grep pem");
	}
	
	public void unsubscribeFromProductID(ProductID pid){
		log.info("Unsubscribing from productID:"+ pid.productId);
		sshCommandRunner.runCommandAndWait("subscription-manager-cli unsubscribe --serial=\""+pid.serialNumber+"\"");
		this.refreshSubscriptions();
		Assert.assertFalse(consumedProductIDs.contains(pid),"Successfully unsubscribed from productID: "+ pid.productId);
	}
	
	
	
	
	// protected methods ************************************************************

	protected boolean poolsNoLongerAvailable(ArrayList<Pool> beforeSubscription, ArrayList<Pool> afterSubscription){
		for(Pool beforePool:beforeSubscription)
			if (afterSubscription.contains(beforePool))
				return false;
		return true;
	}
	
	/**
	 * @param productCerts - stdout from "subscription-manager-cli list"
	 * @return
	 */
	protected ArrayList<HashMap<String,String>> parseAvailableProductCerts(String productCerts){
		/*
		[root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli list
		+-------------------------------------------+
		    Installed Product Status
		+-------------------------------------------+

		ProductName:        	Shared Storage (GFS)     
		Status:             	Not Installed            
		Expires:            	2011-07-01               
		Subscription:       	17                       
		ContractNumber:        	0   
		*/
		
		ArrayList<HashMap<String,String>> productCertList = new ArrayList<HashMap<String,String>>();
		HashMap<String,String> regexes = new HashMap<String,String>();
		
		//			component		pattern
//		regexes.put("productName",	"ProductName:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("status",		"Status:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("expires",		"Expires:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("subscription",	"Subscription:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("productName",	"ProductName:\\s*(.*)");
		regexes.put("status",		"Status:\\s*(.*)");
		regexes.put("expires",		"Expires:\\s*(.*)");
		regexes.put("subscription",	"Subscription:\\s*(.*)");
		
		for(String component:regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(component), Pattern.MULTILINE);
			this.addRegexMatchesToList(pat, productCerts, productCertList, component);
		}
		
		return productCertList;
	}
	
	/**
	 * @param entitlements - stdout from "subscription-manager-cli list --available"
	 * @return
	 */
	protected ArrayList<HashMap<String,String>> parseAvailableEntitlements(String entitlements){
		/*
		[root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli list --available
		+-------------------------------------------+
    		Available Subscriptions
		+-------------------------------------------+
		
		
		Name:              	Basic RHEL Server        
		ProductId:         	MKT-simple-rhel-server-mkt
		PoolId:            	2                        
		quantity:          	10                       
		Expires:           	2011-07-01     * 		/*
		*/

		
		ArrayList<HashMap<String,String>> entitlementList = new ArrayList<HashMap<String,String>>();
		HashMap<String,String> regexes = new HashMap<String,String>();
				
		//			component		pattern
//		regexes.put("poolName",		"Name:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("productSku",	"Product SKU:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("poolId",		"PoolId:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("quantity",		"quantity:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("endDate",		"Expires:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("poolName",		"Name:\\s*(.*)");
		regexes.put("productSku",	"ProductId:\\s*(.*)");
		regexes.put("poolId",		"PoolId:\\s*(.*)");
		regexes.put("quantity",		"[Qq]uantity:\\s*(.*)");	// https://bugzilla.redhat.com/show_bug.cgi?id=612730
		regexes.put("endDate",		"Expires:\\s*(.*)");
		
		for(String component : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(component), Pattern.MULTILINE);
			this.addRegexMatchesToList(pat, entitlements, entitlementList, component);
		}
		
		return entitlementList;
	}
	
	/**
	 * @param products - stdout from "subscription-manager-cli list --consumed"
	 * @return
	 */
	protected ArrayList<HashMap<String,String>> parseConsumedProducts(String products){
		/*
		[root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli list --consumed
		+-------------------------------------------+
    		Consumed Product Subscriptions
		+-------------------------------------------+
		
		
		Name:               	High availability (cluster suite)
		ContractNumber:       	0                        
		SerialNumber:       	17                       
		Active:             	True                     
		Begins:             	2010-07-01               
		Expires:            	2011-07-01   
		*/

		ArrayList<HashMap<String,String>> productList = new ArrayList<HashMap<String,String>>();
		HashMap<String,String> regexes = new HashMap<String,String>();
		
		//			component		pattern
//		regexes.put("productId",	"Name:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("serialNumber",	"SerialNumber:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("orderNumber",	"OrderNumber:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("isActive",		"Active:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("startDate",	"Begins:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("endDate",		"Expires:\\s*([a-zA-Z0-9 ,:()]*)");
		regexes.put("productId",	"Name:\\s*(.*)");
		regexes.put("serialNumber",	"SerialNumber:\\s*(.*)");
		regexes.put("orderNumber",	"OrderNumber:\\s*(.*)");
		regexes.put("isActive",		"Active:\\s*(.*)");
		regexes.put("startDate",	"Begins:\\s*(.*)");
		regexes.put("endDate",		"Expires:\\s*(.*)");
		
		for(String component : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(component), Pattern.MULTILINE);
			this.addRegexMatchesToList(pat, products, productList, component);
		}
		
		return productList;
	}
	
	protected HashMap<String, HashMap<String,String>> parseCerts(String certificates){
		HashMap<String, HashMap<String,String>> productMap = new HashMap<String, HashMap<String,String>>();
		HashMap<String,String> regexes = new HashMap<String,String>();
		
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
	
	protected boolean addRegexMatchesToList(Pattern regex, String to_parse, ArrayList<HashMap<String,String>> matchList, String sub_key){
		Matcher matcher = regex.matcher(to_parse);
		int currListElem=0;
		while (matcher.find()){
			if (matchList.size() < currListElem + 1) matchList.add(new HashMap<String,String>());
			HashMap<String,String> matchMap = matchList.get(currListElem);
			matchMap.put(sub_key, matcher.group(1).trim());
			matchList.set(currListElem, matchMap);
			currListElem++;
		}
		return true;
	}
			
	
	protected boolean addRegexMatchesToMap(Pattern regex, String to_parse, HashMap<String, HashMap<String,String>> matchMap, String sub_key){
        Matcher matcher = regex.matcher(to_parse);
        while (matcher.find()) {
            HashMap<String,String> singleCertMap = matchMap.get(matcher.group(1));
            if(singleCertMap == null){
            	HashMap<String,String> newBranch = new HashMap<String,String>();
            	singleCertMap = newBranch;
            }
            singleCertMap.put(sub_key, matcher.group(2));
            matchMap.put(matcher.group(1), singleCertMap);
        }
        return true;
	}
}
