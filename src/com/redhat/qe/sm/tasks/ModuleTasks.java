package com.redhat.qe.sm.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;
import org.testng.SkipException;

import com.redhat.qe.auto.testng.BzChecker;
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
	
	public List<SubscriptionPool> getCurrentlyAvailableSubscriptionPools() {
		return SubscriptionPool.parse(listAvailable());
	}
	
	public List<ProductSubscription> getCurrentlyConsumedProductSubscriptions() {
		return ProductSubscription.parse(listConsumed());
	}
	
	public List<ProductCert> getCurrentlyInstalledProductCerts() {
		return ProductCert.parse(list());
	}
	
	public List<EntitlementCert> getCurrentEntitlementCerts() {
		sshCommandRunner.runCommandAndWait("find /etc/pki/entitlement/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text");
		String certificates = sshCommandRunner.getStdout();

		return EntitlementCert.parse(certificates);
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
		
		for(ProductSubscription sub : getCurrentlyConsumedProductSubscriptions()){
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
		List<ProductSubscription> before = getCurrentlyConsumedProductSubscriptions();
		log.info("Subscribing to subscription pool: "+pool);
		subscribe(pool.poolId, null, null, null, null);
		List<ProductSubscription> after = getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(after.size() >= before.size() && after.size() > 0,
			"The list of currently consumed product subscriptions has increased (or remained the same when the products from this subscription pool overlap those from a previously subscribed pool) after subscribing to pool: "+pool);
	}
	
	public void subscribeToSubscriptionPoolUsingProductId(SubscriptionPool pool) {
		List<ProductSubscription> before = getCurrentlyConsumedProductSubscriptions();
		log.info("Subscribing to subscription pool: "+pool);
		subscribe(null, pool.productId, null, null, null);
		List<ProductSubscription> after = getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(after.size() > before.size() && after.size() > 0,
			"The list of currently consumed product subscriptions has increased (or remained the same when the products from this subscription pool overlap those from a previously subscribed pool) after subscribing to pool: "+pool);
	}
	
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
		Assert.assertTrue(getCurrentlyConsumedProductSubscriptions().size() > 0, "Successfully subscribed to pool with pool ID: "+ pool.poolId +" and pool name: "+ pool.subscriptionName);
		//TODO: add in more thorough product subscription verification
		// first improvement is to assert that the count of consumedProductIDs is at least one greater than the count of consumedProductIDs before the new pool was subscribed to.
	}
	
	public void subscribeToRegToken(String regtoken) {
		log.info("Subscribing to registration token: "+ regtoken);
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner, "subscription-manager-cli subscribe --regtoken="+regtoken);
		Assert.assertTrue((getCurrentlyConsumedProductSubscriptions().size() > 0),
				"At least one entitlement consumed by regtoken subscription");
	}
	
	/**
	 * Individually subscribe to each of the currently available subscription pools
	 */
	public void subscribeToEachOfTheCurrentlyAvailableSubscriptionPools() {

		for (SubscriptionPool pool : getCurrentlyAvailableSubscriptionPools()) {
			this.subscribeToSubscriptionPoolUsingPoolId(pool);
			Assert.assertTrue(!getCurrentlyAvailableSubscriptionPools().contains(pool),"The available subscription pools no longer contains pool: "+pool);
		}
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=613635 - jsefler 7/14/2010
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {String bugId="613635"; if (BzChecker.getInstance().isBugOpen(bugId)&&invokeWorkaroundWhileBugIsOpen) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			Assert.assertContainsMatch(listAvailable(),"^No Available subscription pools to list$","Asserting that no available subscription pools remain after individually subscribing to them all.");
			return;
		} // END OF WORKAROUND
		
		Assert.assertEquals(listAvailable(),"No Available subscription pools to list","Asserting that no available subscription pools remain after individually subscribing to them all.");
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
		for(ProductSubscription sub : getCurrentlyConsumedProductSubscriptions())
			this.unsubscribeFromProductSubscription(sub);
		Assert.assertEquals(getCurrentlyConsumedProductSubscriptions().size(),0,"Currently no product subscriptions are consumed.");
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
		Assert.assertTrue(!getCurrentlyConsumedProductSubscriptions().contains(productSubscription),"The currently consumed product subscriptions does not contain product: "+productSubscription);
		
		// TODO: Somewhere we may want to assert this type of output is negative test, or maybe assert it here.  However the All/Each tests will want to catch the assertions.
		//201007141716:05.796 - FINE: Stderr: Entitlement Certificate with serial number 301 could not be found.
	}
	
	
	
	
	// protected methods ************************************************************

	protected boolean poolsNoLongerAvailable(ArrayList<SubscriptionPool> beforeSubscription, ArrayList<SubscriptionPool> afterSubscription) {
		for(SubscriptionPool beforePool:beforeSubscription)
			if (afterSubscription.contains(beforePool))
				return false;
		return true;
	}
	

}
