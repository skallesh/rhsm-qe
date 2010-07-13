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
import com.redhat.qe.sm.abstractions.SubscriptionPool;
import com.redhat.qe.sm.abstractions.ProductCert;
import com.redhat.qe.sm.abstractions.ProductSubscription;

public class ModuleTasks {

	protected static Logger log = Logger.getLogger(ModuleTasks.class.getName());
	protected static SSHCommandRunner sshCommandRunner = null;
	

	public ModuleTasks() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public void setSSHCommandRunner(SSHCommandRunner runner) {
		sshCommandRunner = runner;
	}

	protected ArrayList<SubscriptionPool> currentlyAvailableSubscriptionPools = new ArrayList<SubscriptionPool>();
	public ArrayList<SubscriptionPool> getCurrentlyAvailableSubscriptionPools() {
		return currentlyAvailableSubscriptionPools;
	}

	protected ArrayList<ProductSubscription> currentlyConsumedProductSubscriptions = new ArrayList<ProductSubscription>();
	public ArrayList<ProductSubscription> getCurrentlyConsumedProductSubscriptions() {
		return currentlyConsumedProductSubscriptions;
	}

	protected ArrayList<ProductCert> currentlyInstalledProductCerts = new ArrayList<ProductCert>();
	public ArrayList<ProductCert> getCurrentlyInstalledProductCerts() {
		return currentlyInstalledProductCerts;
	}
	
	protected ArrayList<EntitlementCert> currentEntitlementCerts = new ArrayList<EntitlementCert>();
	public ArrayList<EntitlementCert> getCurrentEntitlementCerts() {
		return currentEntitlementCerts;
	}

	public void refreshCurrentSubscriptions(){
		currentlyAvailableSubscriptionPools.clear();
		currentlyConsumedProductSubscriptions.clear();
		currentEntitlementCerts.clear();
		currentlyInstalledProductCerts.clear();
		
		log.info("Refreshing current subscription information...");
		
		// refresh available subscriptions
		for(HashMap<String,String> poolMap : parseAvailableSubscriptions(listAvailable()))
			currentlyAvailableSubscriptionPools.add(new SubscriptionPool(poolMap));
		
		// refresh consumed subscriptions
		for(HashMap<String,String> productMap : parseConsumedProducts(listConsumed()))
			currentlyConsumedProductSubscriptions.add(new ProductSubscription(productMap));
		
		// refresh product certs
		for(HashMap<String,String> prodCertMap : parseInstalledProductCerts(list()))
			currentlyInstalledProductCerts.add(new ProductCert(prodCertMap));
		
		//refresh entitlement certificates
		sshCommandRunner.runCommandAndWait("find /etc/pki/entitlement/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text");
		String certificates = sshCommandRunner.getStdout();
		HashMap<String,HashMap<String,String>> certMap = parseCerts(certificates);
		for(String certKey : certMap.keySet())
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
		
		for(ProductSubscription sub:this.currentlyConsumedProductSubscriptions){
			ArrayList<String> pkgList = new ArrayList<String>();
			for(int i=pkglistBegin;i<packageLines.length;i++){
				String[] splitLine = packageLines[i].split(" ");
				String pkgName = splitLine[0];
				String repoName = splitLine[splitLine.length - 1];
				if(repoName.toLowerCase().contains(sub.productName.toLowerCase()))
					pkgList.add(pkgName);
			}
			pkgMap.put(sub.productName, (String[])pkgList.toArray());
		}
		
		return pkgMap;
	}

	// register module tasks ************************************************************
	
	public void registerToCandlepin(String username, String password, String type, String consumerId, Boolean autosubscribe, Boolean force) {
		
		// assemble the register command
		String										command  = "subscription-manager-cli register";	
		if (username!=null)							command += " --username="+username;
		if (password!=null)							command += " --password="+password;
		if (type!=null)								command += " --type="+type;
		if (consumerId!=null)						command += " --consumerid="+consumerId;
		if (autosubscribe!=null && autosubscribe)	command += " --autosubscribe";
		if (force!=null && force)					command += " --force";
		
		// register
		sshCommandRunner.runCommandAndWait(command);

		// FIXME: may want to assert this output and save or return it.  - jsefler 7/8/2010
		// Stdout: 3f92221c-4b26-4e49-96af-b31abd7bd28c admin admin
		
// Moved to ValidRegistration_Test()
//		Assert.assertEquals(
//				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/key.pem").intValue(), 0,
//						"/etc/pki/consumer/key.pem is present after register");
//		Assert.assertEquals(
//				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/cert.pem").intValue(),0,
//						"/etc/pki/consumer/cert.pem is present after register");
	}
	
//	public void registerToCandlepin(String username, String password){
//		// FIXME may want to make force an optional arg
//		sshCommandRunner.runCommandAndWait("subscription-manager-cli register --username="+username+" --password="+password + " --force");
//
//		Assert.assertEquals(
//				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/key.pem").intValue(), 0,
//						"/etc/pki/consumer/key.pem is present after register");
//		Assert.assertEquals(
//				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/cert.pem").intValue(),0,
//						"/etc/pki/consumer/cert.pem is present after register");
//	}
//	
//	public void registerToCandlepinAutosubscribe(String username, String password){
//		sshCommandRunner.runCommandAndWait("subscription-manager-cli register --username="+username+" --password="+password + " --force --autosubscribe");
//		Assert.assertEquals(
//				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/key.pem").intValue(),0,
//						"/etc/pki/consumer/key.pem is present after register");
//		Assert.assertEquals(
//				sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/cert.pem").intValue(),0,
//						"/etc/pki/consumer/cert.pem is present after register");
//	}
	
	
	// unregister module tasks ************************************************************

	public void unregisterFromCandlepin() {
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
	
	/**
	 * @return stdout from "subscription-manager-cli list --consumed"
	 */
	public String listConsumed() {
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,"subscription-manager-cli list --consumed");
		return sshCommandRunner.getStdout();
	}
	
	/**
	 * @return stdout from "subscription-manager-cli list --available"
	 */
	public String listAvailable() {
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,"subscription-manager-cli list --available");
		return sshCommandRunner.getStdout();
	}
	
	/**
	 * @return stdout from "subscription-manager-cli list"
	 */
	public String list() {
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,"subscription-manager-cli list");
		return sshCommandRunner.getStdout();
	}
	
	// subscribe module tasks ************************************************************

	public void subscribe(String poolId, String productId, String regtoken, String email, String locale) {
		
		// assemble the subscribe command
		String					command  = "subscription-manager-cli subscribe";	
		if (poolId!=null)		command += " --pool="+poolId;
		if (productId!=null)	command += " --product="+productId;
		if (regtoken!=null)		command += " --regtoken="+regtoken;
		if (email!=null)		command += " --email="+email;
		if (locale!=null)		command += " --locale="+locale;
		
		// subscribe
		sshCommandRunner.runCommandAndWait(command);
	}
	
	public void subscribeToProduct(String product) {
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,"subscription-manager-cli subscribe --product="+product);
	}
	
	public void subscribeToSubscriptionPoolUsingPoolId(SubscriptionPool pool) {
		log.info("Subscribing to subscription pool: "+pool);
		subscribe(pool.poolId, null, null, null, null);

		ArrayList<ProductSubscription> before = (ArrayList<ProductSubscription>) getCurrentlyConsumedProductSubscriptions().clone();
		refreshCurrentSubscriptions();
		ArrayList<ProductSubscription> after = (ArrayList<ProductSubscription>) getCurrentlyConsumedProductSubscriptions().clone();

		Assert.assertTrue(after.size() > before.size() && after.size() > 0, "The list of currently consumed product subscriptions has increased after subscribing to pool: "+pool);
	}
	
	public void subscribeToSubscriptionPoolUsingProductId(SubscriptionPool pool) {
		log.info("Subscribing to subscription pool: "+pool);
		subscribe(null, pool.productId, null, null, null);

		ArrayList<ProductSubscription> before = (ArrayList<ProductSubscription>) getCurrentlyConsumedProductSubscriptions().clone();
		refreshCurrentSubscriptions();
		ArrayList<ProductSubscription> after = (ArrayList<ProductSubscription>) getCurrentlyConsumedProductSubscriptions().clone();

		Assert.assertTrue(after.size() > before.size() && after.size() > 0, "The list of currently consumed product subscriptions has increased after subscribing to pool: "+pool);
	}
	
//	public void subscribeToSubscriptionPool(SubscriptionPool pool, boolean withPoolID){
//		log.info("Subscribing to subscription pool: "+ pool);
//		if(withPoolID){
//			log.info("Subscribing to pool with pool ID:"+ pool.subscriptionName);
//			sshCommandRunner.runCommandAndWait("subscription-manager-cli subscribe --pool="+pool.poolId);
//		}
//		else{
//			log.info("Subscribing to pool with pool name:"+ pool.subscriptionName);
//			sshCommandRunner.runCommandAndWait("subscription-manager-cli subscribe --product=\""+pool.productId+"\"");
//		}
//		this.refreshCurrentSubscriptions();
//		Assert.assertTrue(currentlyConsumedProductSubscriptions.size() > 0, "Successfully subscribed to pool with pool ID: "+ pool.poolId +" and pool name: "+ pool.subscriptionName);
//		//TODO: add in more thorough product subscription verification
//		// first improvement is to assert that the count of consumedProductIDs is at least one greater than the count of consumedProductIDs before the new pool was subscribed to.
//	}
	
	public void subscribeToSubscriptionPoolUsingPoolId(SubscriptionPool pool, boolean withPoolID){
		log.info("Subscribing to subscription pool: "+ pool);
		if(withPoolID){
			log.info("Subscribing to pool with pool ID:"+ pool.subscriptionName);
			sshCommandRunner.runCommandAndWait("subscription-manager-cli subscribe --pool="+pool.poolId);
		}
		else{
			log.info("Subscribing to pool with pool name:"+ pool.subscriptionName);
			sshCommandRunner.runCommandAndWait("subscription-manager-cli subscribe --product=\""+pool.productId+"\"");
		}
		this.refreshCurrentSubscriptions();
		Assert.assertTrue(currentlyConsumedProductSubscriptions.size() > 0, "Successfully subscribed to pool with pool ID: "+ pool.poolId +" and pool name: "+ pool.subscriptionName);
		//TODO: add in more thorough product subscription verification
		// first improvement is to assert that the count of consumedProductIDs is at least one greater than the count of consumedProductIDs before the new pool was subscribed to.
	}
	
	public void subscribeToRegToken(String regtoken) {
		log.info("Subscribing to registration token: "+ regtoken);
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner, "subscription-manager-cli subscribe --regtoken="+regtoken);
		this.refreshCurrentSubscriptions();
		Assert.assertTrue((this.currentlyConsumedProductSubscriptions.size() > 0),
				"At least one entitlement consumed by regtoken subscription");
	}
	
	/**
	 * Individually subscribe to each of the currently available subscription pools
	 * @param withPoolID
	 */
	public void subscribeToEachOfTheCurrentlyAvailableSubscriptionPools(/*boolean withPoolID*/) {
//		if (!withPoolID) log.warning("Overriding request to  subscribeToAllPools withPoolID="+withPoolID+" to true."); withPoolID = true;
//		log.info("Subscribing to all pools"+(withPoolID?" (using pool ID)...":"..."));
		this.refreshCurrentSubscriptions();
		ArrayList<SubscriptionPool> availablePools = (ArrayList<SubscriptionPool>)getCurrentlyAvailableSubscriptionPools().clone();
		for (SubscriptionPool pool : availablePools) {
			this.subscribeToSubscriptionPoolUsingPoolId(pool/*,withPoolID*/);
			Assert.assertTrue(!getCurrentlyAvailableSubscriptionPools().contains(pool),"The available subscription pools no longer contains pool: "+pool);
		}
		Assert.assertEquals(listAvailable(),"No Available subscription pools to list","Asserting that no available subscription pools remain after individially subscribing to them all.");
//		Assert.assertTrue(this.poolsNoLongerAvailable(availablePools, this.currentlyAvailableSubscriptionPools),
//				"Pool quantities successfully decremented");
	}
	
	// unsubscribe module tasks ************************************************************

	/**
	 * Issues a call to "subscription-manager-cli unsubscribe" which will unsubscribe from
	 * all currently consumed product subscriptions and then asserts the list --consumed is empty.
	 */
	public void unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions() {
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,"subscription-manager-cli unsubscribe");
		Assert.assertEquals(listConsumed(),"No Consumed subscription pools to list","Successfully unsubscribed from all consumed products.");
	}
	
	/**
	 * Individually unsubscribe from each of the currently consumed product subscriptions.
	 */
	public void unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions() {
		log.info("Unsubscribing from each of the currently consumed product subscriptions...");
		this.refreshCurrentSubscriptions();
		ArrayList<ProductSubscription> consumedProductIDbefore = (ArrayList<ProductSubscription>)this.currentlyConsumedProductSubscriptions.clone();
		for(ProductSubscription sub : consumedProductIDbefore)
			this.unsubscribeFromProductSubscription(sub);
		Assert.assertEquals(this.currentlyConsumedProductSubscriptions.size(),0,"Currently no product subscriptions are consumed.");
		log.info("Verifying that entitlement certificates are no longer present...");
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,
				"ls /etc/pki/entitlement/product/ | grep pem");
	}
	
	/**
	 * Unsubscribe from the given product subscription using its serial number.
	 * @param productSubscription
	 */
	public void unsubscribeFromProductSubscription(ProductSubscription productSubscription) {
		log.info("Unsubscribing from product subscription: "+ productSubscription);
		sshCommandRunner.runCommandAndWait("subscription-manager-cli unsubscribe --serial=\""+productSubscription.serialNumber+"\"");
		this.refreshCurrentSubscriptions();
		Assert.assertTrue(!currentlyConsumedProductSubscriptions.contains(productSubscription),"The currently consumed product subscriptions does not contain product: "+productSubscription);
	}
	
	
	
	
	// protected methods ************************************************************

	protected boolean poolsNoLongerAvailable(ArrayList<SubscriptionPool> beforeSubscription, ArrayList<SubscriptionPool> afterSubscription) {
		for(SubscriptionPool beforePool:beforeSubscription)
			if (afterSubscription.contains(beforePool))
				return false;
		return true;
	}
	
	/**
	 * @param productCerts - stdout from "subscription-manager-cli list"
	 * @return
	 */
	protected ArrayList<HashMap<String,String>> parseInstalledProductCerts(String productCerts) {
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
		
//		regexes.put("productName",	"ProductName:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("status",		"Status:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("expires",		"Expires:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("subscription",	"Subscription:\\s*([a-zA-Z0-9 ,:()]*)");
		
		// ProductCert abstractionField			pattern		(Note: the abstractionField must be defined in the ProductCert class)
		regexes.put("productName",				"ProductName:\\s*(.*)");
		regexes.put("status",					"Status:\\s*(.*)");
		regexes.put("expires",					"Expires:\\s*(.*)");
		regexes.put("subscription",				"Subscription:\\s*(.*)");
		
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			this.addRegexMatchesToList(pat, productCerts, productCertList, field);
		}
		
		return productCertList;
	}
	
	/**
	 * @param entitlements - stdout from "subscription-manager-cli list --available"
	 * @return
	 */
	protected ArrayList<HashMap<String,String>> parseAvailableSubscriptions(String entitlements) {
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
		
//		regexes.put("poolName",		"Name:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("productSku",	"Product SKU:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("poolId",		"PoolId:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("quantity",		"quantity:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("endDate",		"Expires:\\s*([a-zA-Z0-9 ,:()]*)");
		
		// ProductCert abstractionField		pattern		(Note: the abstractionField must be defined in the ProductCert class)
		regexes.put("subscriptionName",		"Name:\\s*(.*)");
		regexes.put("productId",			"ProductId:\\s*(.*)");
		regexes.put("poolId",				"PoolId:\\s*(.*)");
		regexes.put("quantity",				"[Qq]uantity:\\s*(.*)");	// https://bugzilla.redhat.com/show_bug.cgi?id=612730
		regexes.put("endDate",				"Expires:\\s*(.*)");
		
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			this.addRegexMatchesToList(pat, entitlements, entitlementList, field);
		}
		
		return entitlementList;
	}
	
	/**
	 * @param products - stdout from "subscription-manager-cli list --consumed"
	 * @return
	 */
	protected ArrayList<HashMap<String,String>> parseConsumedProducts(String products) {
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
		
//		regexes.put("productId",	"Name:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("serialNumber",	"SerialNumber:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("orderNumber",	"OrderNumber:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("isActive",		"Active:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("startDate",	"Begins:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("endDate",		"Expires:\\s*([a-zA-Z0-9 ,:()]*)");

		// ProductSubscription abstractionField	pattern		(Note: the abstractionField must be defined in the ProductSubscription class)
		regexes.put("productName",				"Name:\\s*(.*)");
		regexes.put("serialNumber",				"SerialNumber:\\s*(.*)");
		regexes.put("contractNumber",			"ContractNumber:\\s*(.*)");
		regexes.put("isActive",					"Active:\\s*(.*)");
		regexes.put("startDate",				"Begins:\\s*(.*)");
		regexes.put("endDate",					"Expires:\\s*(.*)");
		
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			this.addRegexMatchesToList(pat, products, productList, field);
		}
		
		return productList;
	}
	
	protected HashMap<String, HashMap<String,String>> parseCerts(String certificates) {
		HashMap<String, HashMap<String,String>> productMap = new HashMap<String, HashMap<String,String>>();
		HashMap<String,String> regexes = new HashMap<String,String>();
		
		// EntitlementCert abstractionField	pattern		(Note: the abstractionField must be defined in the EntitlementCert class)
		regexes.put("name",					"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.1:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("label",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.2:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("phys_ent",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.3:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("flex_ent",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.4:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("vendor_id",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.5:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("download_url",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.6:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("enabled",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.8:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			this.addRegexMatchesToMap(pat, certificates, productMap, field);
		}
		
		return productMap;
	}
	
	protected boolean addRegexMatchesToList(Pattern regex, String to_parse, ArrayList<HashMap<String,String>> matchList, String sub_key) {
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
			
	
	protected boolean addRegexMatchesToMap(Pattern regex, String to_parse, HashMap<String, HashMap<String,String>> matchMap, String sub_key) {
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
