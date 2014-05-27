package rhsm.cli.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.jul.TestRecords;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author skallesh
 * 
 * 
 */
@Test(groups = { "GuestLimitingTests","Tier2Tests" })
public class GuestLimitingTests extends SubscriptionManagerCLITestScript{
	protected String ownerKey = "";
	protected List<String> providedProductId=null;
	protected String factname="system.entitlements_valid";
	public static final String factValueForSystemCompliance = "valid"; 			
	public static final String factValueForSystemNonCompliance = "invalid"; 	
	public static final String factValueForSystemPartialCompliance = "partial";
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="Verify the compliance status on the server when the host has more than 2 guests",
			groups={"complianceOfHostWithOnlyGuests"},
			enabled=true)
	protected void complianceOfHostWithtwoGuestsAndGuestLimitOfFour()  throws JSONException, Exception {
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null));
		if(clienttasks.getFactValue("virt.is_guest").equals("True")){
			Map<String, String> factsMap = new HashMap<String, String>();
			factsMap.put("virt.is_guest","False");
			factsMap.put(" virt.uuid", "");
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null);
		}
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
		// call Candlepin API to PUT some guestIds onto the host consumer 
		JSONObject jsonData = new JSONObject();
		Map<String,String> attributes = new HashMap<String,String>();
		attributes.clear();
		attributes.put("virtWhoType", "libvirt");
		attributes.put("active", "1");
		int guestLimit=4;
		List<JSONObject> expectedGuestIds=new ArrayList<JSONObject>();
		for(int k=0;k<=guestLimit-1;k++){
			expectedGuestIds.add(createGuestIdRequestBody("test-guestId"+k, attributes));
		}
		jsonData.put("guestIds", expectedGuestIds);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId, jsonData);
		String pool=getGuestlimitPool(String.valueOf(guestLimit));

		installProductCert(providedProductId.get(randomGenerator.nextInt(providedProductId.size())));
		clienttasks.subscribe(null, null, pool, null, null, "1", null, null, null, null, null);
		String compliance = clienttasks.getFactValue(factname);
		//Assert the system compliance 
		Assert.assertEquals(compliance, factValueForSystemCompliance);
	}
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="Verify the compliance status on the server when the host has more than 4 guests",
			groups={"complianceOfHostWithFiveGuests"},
			enabled=true)
	protected void complianceOfHostWithFiveGuestsAndGuestLimitOfFour()  throws JSONException, Exception {
		
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null));
		if(clienttasks.getFactValue("virt.is_guest").equals("True")){
			Map<String, String> factsMap = new HashMap<String, String>();
			factsMap.put("virt.is_guest","False");
			factsMap.put(" virt.uuid", "");
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null);
		}
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
		// call Candlepin API to PUT some guestIds onto the host consumer 
		JSONObject jsonData = new JSONObject();
		Map<String,String> attributes = new HashMap<String,String>();
		attributes.clear();
		attributes.put("virtWhoType", "libvirt");
		attributes.put("active", "1");
		int guestLimit=5;
		List<JSONObject> expectedGuestIds=new ArrayList<JSONObject>();
		for(int k=0;k<=guestLimit;k++){
			expectedGuestIds.add(createGuestIdRequestBody("test-guestId"+k, attributes));
		}
		jsonData.put("guestIds", expectedGuestIds);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId, jsonData);
		System.out.println(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));

		String pool=getGuestlimitPool(String.valueOf(guestLimit-1));
		installProductCert(providedProductId.get(randomGenerator.nextInt(providedProductId.size())));
		clienttasks.subscribe(null, null, pool, null, null, "1", null, null, null, null, null);
		String compliance = clienttasks.getFactValue(factname);
		//Assert the system compliance 
		Assert.assertEquals(compliance, factValueForSystemPartialCompliance);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="Verify that the guest_limit attribute is global across guest-limit subscriptions",
			groups={"complianceOfGuests"},
			enabled=true)
	protected void VerifyGuestLimitIsGlobal()  throws JSONException, Exception {
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null));
		if(clienttasks.getFactValue("virt.is_guest").equals("True")){
			Map<String, String> factsMap = new HashMap<String, String>();
			factsMap.put("virt.is_guest","False");
			factsMap.put(" virt.uuid", "");
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null);
		}
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
		// call Candlepin API to PUT some guestIds onto the host consumer 
		JSONObject jsonData = new JSONObject();
		Map<String,String> attributes = new HashMap<String,String>();
		attributes.clear();
		attributes.put("virtWhoType", "libvirt");
		attributes.put("active", "1");
		int guestLimit=5;
		List<JSONObject> expectedGuestIds=new ArrayList<JSONObject>();
		for(int k=0;k<=guestLimit;k++){
			expectedGuestIds.add(createGuestIdRequestBody("test-guestId"+k, attributes));
		}
		jsonData.put("guestIds", expectedGuestIds);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId, jsonData);
		String pool=getGuestlimitPool("-1");
		installProductCert(providedProductId.get(randomGenerator.nextInt(providedProductId.size())));
		clienttasks.subscribe(null, null, pool, null, null, "1", null, null, null, null, null);
		String compliance = clienttasks.getFactValue(factname);
		//Assert the system compliance 
		Assert.assertEquals(compliance, factValueForSystemCompliance);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="Verify the compliance status on the server when the host has more than 5 guests and one of the guest is reported to be inactive by virt-who",
			groups={"complianceOfHostWithOneInactiveGuest"},
			enabled=true)
	protected void complianceOfHostWithOneOftheGuestReportedInactive()  throws JSONException, Exception {
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null));
		if(clienttasks.getFactValue("virt.is_guest").equals("True")){
			Map<String, String> factsMap = new HashMap<String, String>();
			factsMap.put("virt.is_guest","False");
			factsMap.put(" virt.uuid", "");
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null);
		}
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
		// call Candlepin API to PUT some guestIds onto the host consumer 
		JSONObject jsonData = new JSONObject();
		Map<String,String> attributes = new HashMap<String,String>();
		attributes.clear();
		attributes.put("virtWhoType", "libvirt");
		attributes.put("active", "1");
		int guestLimit=4;
		List<JSONObject> expectedGuestIds=new ArrayList<JSONObject>();
		for(int k=0;k<=guestLimit-1;k++){
			if(k==3){
				attributes.put("active", "0");
			}
			expectedGuestIds.add(createGuestIdRequestBody("test-guestId"+k, attributes));
		}
		jsonData.put("guestIds", expectedGuestIds);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId, jsonData);
		String pool=getGuestlimitPool(String.valueOf(guestLimit));

		installProductCert(providedProductId.get(randomGenerator.nextInt(providedProductId.size())));
		clienttasks.subscribe(null, null, pool, null, null, "1", null, null, null, null, null);
		String compliance = clienttasks.getFactValue(factname);
		//Assert the system compliance 
		Assert.assertEquals(compliance, factValueForSystemCompliance);
	}

	
	protected String getGuestlimitPool(String guestLimit) throws JSONException, Exception {
		String poolId=null;
		for (SubscriptionPool pool : clienttasks
				.getCurrentlyAvailableSubscriptionPools()) {
			String GuestLimitAttribute = CandlepinTasks
					.getPoolProductAttributeValue(sm_clientUsername,
							sm_clientPassword, sm_serverUrl, pool.poolId,
							"guest_limit");
			if((!(GuestLimitAttribute == null))&&(GuestLimitAttribute.equals(guestLimit))){
				poolId=pool.poolId;
				providedProductId=(CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId));
			}
		}
		return poolId;
	}


	protected void installProductCert(String filename) throws IOException {
		String installDir="/root/temp1/";
		client.runCommandAndWait("mkdir " + installDir);
		client.runCommandAndWait("mv " + clienttasks.productCertDir + "/"+ "*" + " " + installDir);
		client.runCommand("mv " + installDir + "/"+ filename +".pem" +" " + clienttasks.productCertDir);
	}

	@AfterGroups(groups = { "setup" }, value = {"complianceOfHostWithOneInactiveGuest","complianceOfHostWithOnlyGuests","complianceOfHostWithFiveGuests","complianceOfGuests"})
	@AfterClass(groups = "setup")
	public void restoreProductCerts() throws IOException {
		client.runCommandAndWait("mv " + "/root/temp1/*.pem" + " "
				+ clienttasks.productCertDir);
		client.runCommandAndWait("rm -rf " + "/root/temp1");
	}
	
	
	public static JSONObject createGuestIdRequestBody(String guestId,Map<String,String> attributes) throws JSONException{
		JSONObject jsonGuestData = new JSONObject();

		if (guestId!=null)	jsonGuestData.put("guestId", guestId);

		Map<String,String> jsonAttribute = new HashMap<String,String>();
		for (String attributeName : attributes.keySet()) {
			if(attributeName.equals("virtWhoType")){
				jsonAttribute.put("virtWhoType", attributes.get(attributeName));
			}if(attributeName.equals("active")){
				jsonAttribute.put("active", attributes.get(attributeName));
			}

			jsonGuestData.put("attributes", jsonAttribute);
		}
		return jsonGuestData;
	}
}

