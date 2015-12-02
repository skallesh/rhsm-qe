package rhsm.cli.tests;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;

import rhsm.base.CandlepinType;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerBaseTestScript;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.OrderNamespace;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.Repo;
import rhsm.data.RevokedCert;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;
/**
 * @author skallesh
 * 
 * 
 */
@Test(groups = { "StorageTests","Tier3Tests" })
public class StorageBandTests extends SubscriptionManagerCLITestScript{
	Map<String, String> factsMap = new HashMap<String, String>();

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify if you attach 1 quantity of subscription that covers 256 GB on a system with 512 GB subscription , installed product is partially subscribed", 
			groups = { "partiallySubscribeStorageBandSubscription"}, enabled = true)
	public void partiallySubscribeStorageBandSubscription(){
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		for(SubscriptionPool pool :getStorageBandSubscriptions()){
			clienttasks.subscribe(null, null, pool.poolId, null, null, "1", null, null, null, null, null, null);
		}	
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", "908", clienttasks.getCurrentlyInstalledProducts());
		if(installedProduct.productId.equals("908"))
			Assert.assertEquals(installedProduct.status, "Partially Subscribed");
	}
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify if you attach 2 quantity of subscription that covers 256 GB on a system with 512 GB subscription , installed product is fully subscribed", 
			groups = { "FullySubscribeStorageBandSubscription"}, enabled = true)
	public void FullySubscribeStorageBandSubscription(){
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		for(SubscriptionPool pool :getStorageBandSubscriptions()){
			clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null, null);
		}	
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", "908", clienttasks.getCurrentlyInstalledProducts());
		if(installedProduct.productId.equals("908"))
			Assert.assertEquals(installedProduct.status, "Subscribed");
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify if you auto-attach , installed product is fully subscribed", 
			groups = { "AutoAttachStorageBandSubscription"}, enabled = true)
	public void AutoAttachStorageBandSubscription(){
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		clienttasks.subscribe(true, null, (String)null, null, null, null, null, null, null, null, null, null);	
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", "908", clienttasks.getCurrentlyInstalledProducts());
		if(installedProduct.productId.equals("908"))
			Assert.assertEquals(installedProduct.status, "Subscribed");
	}
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify if you auto-heal , installed product is fully subscribed", 
			groups = { "AutoHealStorageBandSubscription"}, enabled = true)
	public void AutoHealStorageBandSubscription(){
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		clienttasks.autoheal(null, true, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", "908", clienttasks.getCurrentlyInstalledProducts());
		if(installedProduct.productId.equals("908"))
			Assert.assertEquals(installedProduct.status.trim(), "Subscribed");
	}
	
	 public List<SubscriptionPool> getStorageBandSubscriptions() {
		 List<SubscriptionPool> StorageBandPools= new ArrayList<SubscriptionPool>();
		 for(SubscriptionPool pool:clienttasks.getAvailableSubscriptionsMatchingInstalledProducts()){
			 if(pool.subscriptionName.toLowerCase().contains("storage") || pool.productId.toLowerCase().contains("storage")){
				 StorageBandPools.add(pool) ;
			 }
		 }
		 if (StorageBandPools.isEmpty()) throw new SkipException("Could not find any Storage Limited subscription pools/SKUs.");
		 
		return StorageBandPools;
	}
	 
	 
	
	@BeforeClass(groups={"setup"})
	public void CustomiseCephFacts() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null);
		factsMap.clear();
		factsMap.put("band.storage.usage", "512");
		clienttasks.createFactsFileWithOverridingValues(factsMap);

	}
	@BeforeClass(groups={"setup"})
	public void RemoveCephFacts() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null);
		clienttasks.removeAllFacts();

	}
}
