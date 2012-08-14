package com.redhat.qe.sm.cli.tests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;


/**
 * @author skallesh
 *
 *
 */
@Test(groups={"BugzillaTests"})
public class BugzillaTests extends SubscriptionManagerCLITestScript {
	
	// Bugzilla Healing Test methods ***********************************************************************

	// Healing Candidates for an automated Test:
	// TODO Cases in Bug 710172 - [RFE] Provide automated healing of expiring subscriptions//done
	// TODO   subcase Bug 746088 - autoheal is not super-subscribing on the day the current entitlement cert expires //done
	// TODO   subcase Bug 746218 - auto-heal isn't working for partial subscription //done
	// TODO Cases in Bug 726411 - [RFE] Support for certificate healing

	/**
	 * @author skallesh
	 * @throws JSONException 
	 * @throws Exception
	 */
	@Test(	description="Auto-heal for partial subscription",
			groups={"autohealPartial","blockedByBug-746218"},
			enabled=false)	//TODO commit to true after the logic in this test is re-implemented
	public void VerifyAutohealForPartialSubscription() throws Exception {
		Integer healFrequency=2;
		String productId=null;
		String poolid=null;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(String)null,null, null, true,null,null, null, null);
		// TODO At this point you should be fully subscribed.  I don't believe you are testing that auto-heal will complete a partially subscribed product
		for (SubscriptionPool pool  : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if(pool.multiEntitlement){
				for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
					if((installedProduct.productName).contains(pool.subscriptionName)){		// TODO this is only true by coincidence of the naming used in our TESTDATA
						poolid=pool.poolId;
					}
							
				}
			}
		clienttasks.restart_rhsmcertd(null, healFrequency, false,null);
		clienttasks.unsubscribe(true, null, null, null, null); 
		clienttasks.subscribe_(null, null, poolid, null, null,null, null, null, null, null, null);
						
	}
		// TODO at this point you need to assert that there is some "Partially Subscribed" product(s), otherwise you are not testing that the next trigger of the healFrequency will finish autosubscribing your system to be fully "Subscribed".
		SubscriptionManagerCLITestScript.sleep(healFrequency*60*1000);
		
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			if((installedProduct.productName).equals(productId))		// TODO Why are you comparing the name to the id?	// TODO productId is always null; there is no assignment, therefore there is no assertion and this test will always pass for no good reason
			Assert.assertEquals(installedProduct.status, "Subscribed");
		}
	}
	
	/**
	 * @author skallesh
	 * @throws JSONException 
	 * @throws Exception
	 */
	@Test(	description="Auto-heal with SLA",
			groups={"AutoHealWithSLA"},
			enabled=true)	
	public void VerifyAutohealWithSLA() throws JSONException, Exception {
		Integer healFrequency=2;
		String availableService = null;	
		String filename=null;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		for (List<Object> availableServiceLevelData : getAllAvailableServiceLevelDataAsListOfLists()) {
			availableService= ((String)availableServiceLevelData.get(1));
		} // TODO Shwetha, what is the purpose of this loop?
		clienttasks.subscribe(true, availableService, (String)null, null, null,null, null, null, null, null, null);
		clienttasks.service_level_(null, null, null, null, null,availableService,null,null, null, null);		
		clienttasks.restart_rhsmcertd(null, healFrequency, false, null);
		clienttasks.unsubscribe(true, null, null, null, null);
		SubscriptionManagerCLITestScript.sleep(healFrequency*60*1000);
		List<EntitlementCert> certs = clienttasks.getCurrentEntitlementCerts();
		if(certs.size()!=0){
		Assert.assertTrue((certs.size()!=0),"autoheal is succesfull with Service level"+availableService); 
		}else
		Assert.assertTrue((certs.size()==0),"There are no products of serviceLevel "+availableService); 
		moveProductCertFiles(filename,false);
	
}
	
	
	/**
	 * @author skallesh
	 * @throws Exception 
	 */
	@Test(	description="verfying Auto-heal when auto-heal parameter is turned off",
			groups={"AutohealTurnedOff"},
			enabled=false)	//TODO commit to true after executing successfully or blockedByBug is open
	
	
	public void AutohealTurnedOff() throws Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		JSONObject jsonConsumer = CandlepinTasks.setAutohealForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, consumerId,false);
		Assert.assertFalse(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled off (expected value=false).");
		Integer healFrequency=2;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		clienttasks.subscribe(true, null, (String)null, null, null,null, null, null, null, null, null);
		clienttasks.restart_rhsmcertd(null, healFrequency, true, null);
		clienttasks.unsubscribe(true, null, null, null, null);
		SubscriptionManagerCLITestScript.sleep(healFrequency*60*1000);
		List<EntitlementCert> certs = clienttasks.getCurrentEntitlementCerts();
		Assert.assertTrue((certs.size()!=0),"autoheal is successful"); 
		
	}
	
	/**
	 * @author skallesh
	 */
	@Test(	description="Auto-heal for Expired subscription",
			groups={"AutohealForExpired","blockedByBug-746088"},
			enabled=false)	//TODO commit to true after executing successfully or blockedByBug is open
	@ImplementsNitrateTest(caseId=119327)
	
	public void VerifyAutohealForExpiredSubscription() {
		Integer healFrequency=2;
		String expireDate = null;
		String startDate= null;
		Calendar date = null;
		String productId=null;
		String min=null;
		String[] newminutes;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		clienttasks.subscribe(true, null, (String)null, null, null,null, null, null, null, null, null);
		for(InstalledProduct installedProducts : clienttasks.getCurrentlyInstalledProducts()){
			if((installedProducts.status).equals("Subscribed")){
				date = installedProducts.endDate;	
				productId=installedProducts.productId;
				expireDate=new SimpleDateFormat("MM-dd-yyyy").format(date.getTime());
				startDate=new SimpleDateFormat("MM-dd-yyyy").format(date.getTime());
			}
		}
		
	/*	String[] dates=expireDate.split("-");
		int exp=Integer.parseInt(dates[1])+1;
	//	String result=clienttasks.setAndGetDate(null, true, null).getStdout();
	//	String[] NewStartDate=result.split(" ");
		newminutes=NewStartDate[3].split(":");
		min =newminutes[0]+newminutes[1]; 
		String newdate=dates[0]+"0"+exp+min+dates[2];
	//	clienttasks.setAndGetDate(true, null, newdate);
		dates=startDate.split("-");
		System.out.println("startDate is "+startDate);
		for(InstalledProduct installedProducts : clienttasks.getCurrentlyInstalledProducts()){
			if((installedProducts.productId).equals(productId)){
				Assert.assertEquals(installedProducts.status, "Expired");
			}
			
		}
	
		clienttasks.restart_rhsmcertd(null, healFrequency, true, null);
		//SubscriptionManagerCLITestScript.sleep(healFrequency*60*1000);
		List<EntitlementCert> certs = clienttasks.getCurrentEntitlementCerts();
		Assert.assertTrue((certs.size()!=0),"autoheal is successful"); 
		result=clienttasks.setAndGetDate(null, true, null).getStdout();
		NewStartDate=result.split(" ");
		newminutes=NewStartDate[4].split(":");
		min =newminutes[0]+newminutes[1]; 
		newdate=dates[0]+"0"+exp+min+dates[2];
		System.out.println("new date  "+ newdate);
		clienttasks.setAndGetDate(true, null, newdate);
		*/

		
	}
	/**
	 * @author skallesh
	 * @throws Exception 
	 * @throws JSONException 
	 */
	@Test(	description="Auto-heal for subscription",
			groups={"AutoHeal"},
			enabled=false)	//TODO commit to true after executing successfully or blockedByBug is open
	@ImplementsNitrateTest(caseId=119327)
	
	public void VerifyAutohealForSubscription() throws JSONException, Exception {
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null,true, null, null, null, null));
		
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertFalse(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled off (expected value=false).");

		Integer healFrequency=2;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		clienttasks.subscribe(true, null, (String)null, null, null,null, null, null, null, null, null);
		clienttasks.restart_rhsmcertd(null, healFrequency, true, null);
		clienttasks.unsubscribe(true, null, null, null, null);
		SubscriptionManagerCLITestScript.sleep(healFrequency*60*1000);
		List<EntitlementCert> certs = clienttasks.getCurrentEntitlementCerts();
		Assert.assertTrue((certs.size()!=0),"autoheal is successful"); 
	}
	
	/**
	 * @author skallesh
	 * @throws JSONException
	 * @throws Exception
	 */
	@Test(	description="Auto-heal with SLA",
			groups={"AutoHealFailFOrSLA"},
			enabled=true)	
	public void VerifyAutohealFailForSLA() throws JSONException, Exception {
		Integer healFrequency=2;
		String availableService = null;	
		String filename=null;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		for (List<Object> availableServiceLevelData : getAllAvailableServiceLevelDataAsListOfLists()) {
			availableService= ((String)availableServiceLevelData.get(1));
		} // TODO Shwetha, what is the purpose of this loop?
		clienttasks.subscribe(true, availableService, (String)null, null, null,null, null, null, null, null, null);
		List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		List <ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		for (ProductCert productCert : productCerts) {
			InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(productCert,installedProducts);
			
			if(installedProduct.status.toString().equalsIgnoreCase("Subscribed")){
				 filename=installedProduct.productId+".pem";
				moveProductCertFiles(filename,true);
				
			}
		}
		
		clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null));
		
		clienttasks.service_level_(null, null, null, null, null,availableService,null,null, null, null);		
		clienttasks.restart_rhsmcertd(null, healFrequency, false, null);
		clienttasks.unsubscribe(true, null, null, null, null);
		clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "entitlementCertDir");
		SubscriptionManagerCLITestScript.sleep(healFrequency*60*1000);
		
		List<EntitlementCert> certs = clienttasks.getCurrentEntitlementCerts();
		Assert.assertTrue((certs.size()==0),"autoheal has failed"); 
		moveProductCertFiles(filename,false);
	}
	
	
	

	
	
	
	
	// Bugzilla Subscribe Test methods ***********************************************************************
	
	// Subscribe Candidates for an automated Test:
	// TODO Bug 668032 - rhsm not logging subscriptions and products properly //done --shwetha
	// TODO Bug 670831 - Entitlement Start Dates should be the Subscription Start Date //Done --shwetha
	// TODO Bug 664847 - Autobind logic should respect the architecture attribute //working on
	// TODO Bug 676377 - rhsm-compliance-icon's status can be a day out of sync - could use dbus-monitor to assert that the dbus message is sent on the expected compliance changing events
	// TODO Bug 739790 - Product "RHEL Workstation" has a valid stacking_id but its socket_limit is 0
	// TODO Bug 707641 - CLI auto-subscribe tries to re-use basic auth credentials.
	
	// TODO Write an autosubscribe bug... 1. Subscribe to all avail and note the list of installed products (Subscribed, Partially, Not) 
	//									  2. Unsubscribe all  3. Autosubscribe and verfy same installed product status (Subscribed, Not)//done --shwetha
	// TODO Bug 746035 - autosubscribe should NOT consider existing future entitlements when determining what pools and quantity should be autosubscribed //working on
	// TODO Bug 747399 - if consumer does not have architecture then we should not check for it
	// TODO Bug 743704 - autosubscribe ignores socket count on non multi-entitle subscriptions //done --shwetha
	// TODO Bug 740788 - Getting error with quantity subscribe using subscription-assistance page 
	//                   Write an autosubscribe test that mimics partial subscriptions in https://bugzilla.redhat.com/show_bug.cgi?id=740788#c12
	// TODO Bug 720360 - subscription-manager: entitlement key files created with weak permissions // done --shwetha
	// TODO Bug 772218 - Subscription manager silently rejects pools requested in an incorrect format.//done --shwetha

	/**
	 * @author skallesh
	 */
	@Test(   description="subscription-manager: subscribe multiple pools in incorrect format",
			              groups={"MysubscribeTest","blockedByBug-772218"},
			              enabled=false)	//TODO commit to true after executing successfully or blockedByBug is open
			      public void VerifyIncorrectSubscriptionFormat() {
			 clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null,
			 null, null);
			          List <String> poolIds = new ArrayList<String>();
			          for (SubscriptionPool pool :
			 clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			          poolIds.add(pool.poolId);
			          }
			          int j=randomGenerator.nextInt(poolIds.size());
			          int i=randomGenerator.nextInt(poolIds.size());
			          if(i==j){
			        	j=randomGenerator.nextInt(poolIds.size());
			          }else{
			          SSHCommandResult subscribeResult =
			 subscribeInvalidFormat_(null,null,poolIds.get(i),poolIds.get(j),null,null,null,null, null, null, null, null);
			          Assert.assertEquals(subscribeResult.getStdout().trim(), "cannot parse argument: "+poolIds.get(j) );
			      }}
	
	
	/**
	 * @author skallesh
	 * @throws Exception 
	 */
	@Test(    description="Verify that Entitlement Start Dates is the Subscription Start Date ",
            groups={"VerifyEntitlementStart_Test"},
             enabled=false)	//TODO commit to true after executing successfully or blockedByBug is open
	public void VerifyEntitlementStart_Test() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, false, null, null, null);
		for(SubscriptionPool pools:clienttasks.getCurrentlyAvailableSubscriptionPools()){
			Calendar end_date=pools.endDate;
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/pools/"+pools.poolId));	
			String expireDate=new SimpleDateFormat("yyyy-MM-dd").format(end_date.getTime());
			String startdate=jsonPool.getString("endDate");
			String[] split_word=startdate.split("T");
		    Assert.assertEquals(split_word[0], expireDate);
			
		}
		}
		
	/**
	 * @author skallesh
	 * @throws Exception 
	 */
	@Test(    description="Verify if rhsm not logging subscriptions and products properly ",
            groups={"VerifyarchitectureForAutobind_Test"},
         //   dataProvider="getAllFutureSystemSubscriptionPoolsData",
            enabled=true)
	public void VerifyarchitectureForAutobind_Test() throws Exception{
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, false, null, null, null);
		Map<String, String> result=clienttasks.getFacts();
		String arch=result.get("uname.machine");
		List<String> cpu_arch=new ArrayList<String>();
		String input ="x86_64|i686|ia64|ppc|ppc64|s390x|s390";
		String[] values=input.split("\\|");
		Boolean flag=false;
		Boolean expected=true;
		for(int i=0;i<values.length;i++){
			cpu_arch.add(values[i]);
		}
        
		
		Pattern p = Pattern.compile(arch);
        Matcher  matcher = p.matcher(input);
        while (matcher.find()){
        String pattern_=matcher.group();
        cpu_arch.remove(pattern_);
       
        }
		String architecture=cpu_arch.get(randomGenerator.nextInt(cpu_arch.size()));
		for(SubscriptionPool pool:clienttasks.getCurrentlyAvailableSubscriptionPools()){
			if((pool.subscriptionName).contains(" "+architecture)){
				flag=true;
				Assert.assertEquals(flag, expected);
			}
				
			}
		
		for(SubscriptionPool pools:clienttasks.getCurrentlyAllAvailableSubscriptionPools()){
			if((pools.subscriptionName).contains(architecture)){
				flag=true;
				Assert.assertEquals(flag, expected);
			}
				
			}
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("uname.machine", String.valueOf(architecture));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts_(null, true, null, null, null);
		}
					
	
	/**
	 * @author skallesh
	 * @throws Exception 
	 */
	@Test(    description="Verify if rhsm not logging subscriptions and products properly ",
            groups={"VerifyRhsmLogging_Test"},
         //   dataProvider="getAllFutureSystemSubscriptionPoolsData",
            enabled=false)	//TODO commit to true after executing successfully or blockedByBug is open
	public void VerifyRhsmLogging_Test() throws Exception{
		Boolean actual=true;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, false, null, null, null);
		List<SubscriptionPool> result=clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		for(SubscriptionPool pool :result){
			if((pool.subscriptionName).contains("Bundled")){
				clienttasks.subscribe(null, null,pool.poolId, null, null, null, null, null, null, null, null);
		}}
		
		Boolean flag=clienttasks.waitForRegexInRhsmLog("@ /etc/pki/entitlement");
		Assert.assertEquals(flag, actual);
					
	}
	/**
	 * @author skallesh
	 * @throws Exception 
	 */
	@Test(    description="Verify if the status of installed products match when autosubscribed,and when you subscribe all the available products ",
            groups={"VerifyFuturesubscription_Test"},
         //   dataProvider="getAllFutureSystemSubscriptionPoolsData",
            enabled=true)
	public void VerifyFuturesubscription_Test() throws Exception{
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, false, null, null, null);
		Calendar now = new GregorianCalendar();
		List<String> productname=new ArrayList<String>();
		String ProductIds=null;
		JSONObject futureJSONPool = null;
		now.setTimeInMillis(System.currentTimeMillis());
		for (List<Object> l : getAllFutureJSONPoolsDataAsListOfLists(ConsumerType.system)) {
			futureJSONPool = (JSONObject) l.get(0);
		}
		Calendar onDate = parseISO8601DateString(futureJSONPool.getString("startDate"),"GMT"); 
		onDate.add(Calendar.DATE, 1);
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String onDateToTest = yyyy_MM_dd_DateFormat.format(onDate.getTime());
		for(InstalledProduct installed  : clienttasks.getCurrentlyInstalledProducts()){
			productname.add(installed.productName);
			
		}
		List<String> FuturePool = listFutureSubscription_OnDate(true,onDateToTest);
		for(String result:FuturePool){
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			ProductIds=installedProduct.productName;
			 if(!(installedProduct.status.equals( "Future Subscription")))
				 clienttasks.subscribe(null, null,result, null, null, null, null, null, null, null, null);							
						
		}}
	 	clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			if(installedProduct.productName==ProductIds){
				Assert.assertEquals(installedProduct.status, "Subscribed");
	}
		}}

	
	/**
	 * @author skallesh
	 */
	@Test(    description="Verify if the status of installed products match when autosubscribed,and when you subscribe all the available products ",
            groups={"Verifyautosubscribe_Test","blockedByBug-844455"},
            enabled=true)
	public void Verifyautosubscribe_Test(){
		int socket=4;
		Map<String,String> factsMap = new HashMap<String,String>();
		Integer moreSockets = 4;
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(moreSockets));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		InstalledProduct installedProductAfterAuto = null;
		InstalledProduct installedProduct = null;
     clienttasks.register_(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
	 clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
	 List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		List <ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		for (ProductCert productCert : productCerts) {
			installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(productCert,installedProducts);
		}
		clienttasks.unsubscribe(true, null, null, null, null);
		clienttasks.subscribe(true,null,(String)null,null,null, null, null, null, null, null, null);
		 List <InstalledProduct> installedProductsAfterAuto = clienttasks.getCurrentlyInstalledProducts();
			List <ProductCert> productCertsAfterAuto = clienttasks.getCurrentProductCerts();
			for (ProductCert productCertAfterAuto : productCertsAfterAuto) {
				installedProductAfterAuto = clienttasks.getInstalledProductCorrespondingToProductCert(productCertAfterAuto,installedProductsAfterAuto);
			}
			Assert.assertEquals(installedProduct ,installedProductAfterAuto,"no change in the status");
	}
		
	
	
	/**
	 * @author skallesh
	 * @throws Exception 
	 */
	@Test(    description="Verify if autosubscribe ignores socket count on non multi-entitled subscriptions ",
            groups={"VerifyautosubscribeIgnoresSocketCount_Test"},
            enabled=false)	//TODO commit to true after executing successfully or blockedByBug is open
	public void VerifyautosubscribeIgnoresSocketCount_Test() throws Exception{
		int socketnum = 0;
		int socketvalue=0;
		List<String> SubscriptionId = new ArrayList<String>();
		clienttasks.register_(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		for(SubscriptionPool SubscriptionPool: clienttasks.getCurrentlyAllAvailableSubscriptionPools()){
		 if(!(SubscriptionPool.multiEntitlement)){
			 SubscriptionId.add(SubscriptionPool.subscriptionName);
				String poolProductSocketsAttribute = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, SubscriptionPool.poolId, "sockets");
				if(!(poolProductSocketsAttribute==null)){
					socketvalue=Integer.parseInt(poolProductSocketsAttribute);
					if (socketvalue > socketnum) {
						socketnum = socketvalue;
		               }
				}else{
					socketvalue=0;
				}
		
			}
		 Map<String,String> factsMap = new HashMap<String,String>();
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(socketnum+2));
			clienttasks.createFactsFileWithOverridingValues(factsMap);
	
		}
		clienttasks.subscribe(true,null,(String)null,null,null, null, null, null, null, null, null);
		for (InstalledProduct installedProductsAfterAuto :clienttasks.getCurrentlyInstalledProducts()) {
				for(String pool:SubscriptionId){
					if(installedProductsAfterAuto.productName.contains(pool))
				
						if((installedProductsAfterAuto.status).equalsIgnoreCase("Subscribed")){
						Assert.assertEquals("Subscribed", (installedProductsAfterAuto.status).trim(), "test  has failed");
						}
				}
			}
	}
	
	
	/**
	 * @author skallesh
	 */
	@Test(    description="subscription-manager: entitlement key files created with weak permissions",
            groups={"MykeyTest","blockedByBug-720360"},
            enabled=true)
    public void VerifyKeyFilePermissions() {
        clienttasks.register_(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
        clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
        String subscribeResult=getEntitlementCertFilesWithPermissions();
        Pattern p = Pattern.compile("[,\\s]+");
        String[] result = p.split(subscribeResult);
        for (int i=0; i<result.length; i++){
               Assert.assertEquals(result[i], "-rw-------","permission for etc/pki/entitlement/<serial>-key.pem is -rw-------" );
        i++;
    }}
	
	
	
	
	
	// Configuration methods ***********************************************************************

	protected Integer configuredHealFrequency = null;
	@BeforeClass (groups="setup")
	public void rememberConfiguredHealFrequency() {
		if (clienttasks==null) return;
		configuredHealFrequency	= Integer.valueOf(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsmcertd", "healFrequency"));
	}
	
	@AfterClass (groups="setup")
	public void restoreConfiguredHealFrequency() {
		if (clienttasks==null) return;
		clienttasks.restart_rhsmcertd(null, configuredHealFrequency, false, null);
	}
	
	@AfterClass (groups="setup")
	public void restoreProductCerts() {
		if (clienttasks==null) return;
		moveProductCertFiles(null,false);
	}
	
	
	
	
	// Protected methods ***********************************************************************
	
	protected void moveProductCertFiles(String filename,Boolean move) {
		
		//sshCommandRunner.runCommandAndWait("find /etc/pki/product/ -name '*.pem'");
		client.runCommandAndWait("mkdir -p "+"/etc/pki/tmp1");
		if(move==true){
			client.runCommandAndWait("mv "+clienttasks.productCertDir+"/"+filename+" "+"/etc/pki/tmp1/");
				}else
					client.runCommandAndWait("mv "+ "/etc/pki/tmp/*.pem"+" " +clienttasks.productCertDir);
		client.runCommandAndWait("rm -rf "+ "/etc/pki/tmp1");
	}


	protected String getEntitlementCertFilesWithPermissions() {
		String lsFiles =client.runCommandAndWait("ls -l "+clienttasks.entitlementCertDir+"/*-key.pem" + " | cut -d "+"' '"+" -f1,9" ).getStdout();
		return lsFiles;
	}
	
	
	protected SSHCommandResult subscribeInvalidFormat_(Boolean auto, String servicelevel, String poolIdOne, String poolIdTwo,List<String> productIds, List<String> regtokens, String quantity, String email, String locale,
			 String proxy, String proxyuser, String proxypassword) {
			
			       
			          String command = clienttasks.command;         command += " subscribe";
			          if (poolIdOne!=null && poolIdTwo !=null)
			          command += " --pool="+poolIdOne +" "+poolIdTwo;
			
			              // run command without asserting results
			          return client.runCommandAndWait(command);
			      }
	
	
	protected List<String> listFutureSubscription_OnDate(Boolean available,String ondate){
		List<String> PoolId=new ArrayList<String>();
		SSHCommandResult result=clienttasks.list_(true, true, null, null, null, ondate, null, null, null);
		List<SubscriptionPool> Pool = SubscriptionPool.parse(result.getStdout());
		for(SubscriptionPool availablePool:Pool){
			if(availablePool.multiEntitlement){
				PoolId.add(availablePool.poolId);
			}
		}
		
		return PoolId;
	}
	
	// Data Providers ***********************************************************************

}
