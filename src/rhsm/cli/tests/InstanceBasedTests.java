package rhsm.cli.tests;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author skallesh
 *
 *
 */

@Test(groups={"InstanceBasedTests"})
public class InstanceBasedTests extends SubscriptionManagerCLITestScript {
	Map<String, String> factsMap = new HashMap<String, String>();
	protected Integer configuredHealFrequency = null;
	protected String configuredHostname=null;

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Subscription of instance based subscription", 
			groups = { "SubscriptionOfInstanceBasedTest"}, enabled = true)
	public void SubscriptionOfInstanceBasedTest() throws JSONException,Exception {
		
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		
		if(clienttasks.getFactValue("virt.is_guest").contains("false")){
			String socket=clienttasks.getFactValue("cpu.cpu_socket(s)");
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
						if(availList.subscriptionName.contains("Instance Based")){
							clienttasks.subscribe(null, null, availList.poolId, null, null, socket, null, null, null, null, null);
					
			}
		
		 }
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productId.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Subscribed");
					
				}	
		}
	}if(clienttasks.getFactValue("virt.is_guest").contains("true")){
		for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			if(availList.subscriptionName.contains("Instance Based")){
				clienttasks.subscribe(null, null, availList.poolId, null, null, null, null, null, null, null, null);
		
}

}
for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
	if(installed.productId.contains("Instance Server")){
		Assert.assertEquals(installed.status.trim(), "Subscribed");
		
	}	
}
	}
		
}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Healing of instance based subscription", 
			groups = { "HealingOfInstanceBasedSubscription"}, enabled = true)
	public void HealingOfInstanceBasedSubscription() throws JSONException,Exception {
		
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		int healFrequency=2;
		
		if(clienttasks.getFactValue("virt.is_guest").contains("false")){
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
			clienttasks.facts(null, true, null, null, null);
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
						if(availList.subscriptionName.contains("Instance Based")){
							clienttasks.subscribe(null, null, availList.poolId, null, null, "2", null, null, null, null, null);
					
			}
		
		 }
			String messageDetails="Only covers 2 of "+sockets+" sockets.";
		
			for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productId.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
					Assert.assertEquals(installed.statusDetails.trim(), messageDetails);
				}
			}
		}
		clienttasks.restart_rhsmcertd(null, healFrequency, false, null);
		SubscriptionManagerCLITestScript.sleep(healFrequency * 60 * 1000);
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productName.contains("Instance Server")){
				Assert.assertEquals(installed.status.trim(), "Subscribed");
		}
	}
		if(clienttasks.getFactValue("virt.is_guest").contains("true")){
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
			clienttasks.facts(null, true, null, null, null);
			clienttasks.restart_rhsmcertd(null, healFrequency, false, null);
			SubscriptionManagerCLITestScript.sleep(healFrequency * 60 * 1000);
			for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productName.contains("Instance Server")){
				Assert.assertEquals(installed.status.trim(), "Subscribed");
		}
	}
			for(ProductSubscription consumed:clienttasks.getCurrentlyConsumedProductSubscriptions()){
				if(consumed.productName.contains("Instance Based")){
				Assert.assertEquals(consumed.quantityUsed, "1");
			}}
	}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Auto bind of instance based subscription", 
			groups = { "AutoBindingInstanceBasedSubscription"}, enabled = true)
	public void AutoBindingInstanceBasedSubscription() throws JSONException,Exception {
		
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		if(clienttasks.getFactValue("virt.is_guest").contains("false")){
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
			clienttasks.facts(null, true, null, null, null);
			clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
			for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productName.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Subscribed");
			}
		}
			for(ProductSubscription consumed:clienttasks.getCurrentlyConsumedProductSubscriptions()){
				if(consumed.productName.contains("Instance Based")){
				Assert.assertEquals(consumed.quantityUsed, "sockets");
			}}
			
			}if(clienttasks.getFactValue("virt.is_guest").contains("true")){
				Integer sockets = 4;
				factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
				clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
				clienttasks.facts(null, true, null, null, null);
				clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
				for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productName.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Subscribed");
			}
		}
				for(ProductSubscription consumed:clienttasks.getCurrentlyConsumedProductSubscriptions()){
					if(consumed.productName.contains("Instance Based")){
					Assert.assertEquals(consumed.quantityUsed, "1");
				}}
		}
		}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Auto bind of instance based subscription", 
			groups = { "StackingOfInstanceBasedSubscription"}, enabled = true)
	public void StackingOfInstanceBasedSubscription() throws JSONException,Exception {
		
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		
		if(clienttasks.getFactValue("virt.is_guest").contains("false")){
			Integer sockets = 4;
			String poolId=null;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
			clienttasks.facts(null, true, null, null, null);
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
						if(availList.subscriptionName.contains("Instance Based")){
							poolId= availList.poolId;
							clienttasks.subscribe(null, null, availList.poolId, null, null, "2", null, null, null, null, null);
					
			}
		
		 }
			String messageDetails="Only covers 2 of "+sockets+" sockets.";
		
			for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productId.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
					Assert.assertEquals(installed.statusDetails.trim(), messageDetails);
				}
			}
			clienttasks.subscribe(null, null, poolId, null, null, "2", null, null, null, null, null);
			for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productId.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Subscribed");
					
				}	
		}
	}if(clienttasks.getFactValue("virt.is_guest").contains("true")){
		Integer sockets = 4;
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
		clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
		clienttasks.facts(null, true, null, null, null);
		for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
					if(availList.subscriptionName.contains("Instance Based")){
						clienttasks.subscribe(null, null, availList.poolId, null, null, "1", null, null, null, null, null);
				
		}
	
	 }
		
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productId.contains("Instance Server")){
				Assert.assertEquals(installed.status.trim(), "Subscribed");
				
			}	
	}
}
	
	}
	
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Auto bind of instance based subscription", 
			groups = { "StackingOfInstanceBasedSubscription"}, enabled = true)
	public void AutoAttachOfInstanceBasedSubscriptionOnCertV1() throws JSONException,Exception {
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("system.certificate_version", "1.0");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		clienttasks.facts(null, true, null, null, null);
		if(clienttasks.getFactValue("virt.is_guest").contains("false")){
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
			clienttasks.facts(null, true, null, null, null);
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
						if(availList.subscriptionName.contains("Instance Based")){
							clienttasks.subscribe(null, null, availList.poolId, null, null, "2", null, null, null, null, null);
					
			}
		
		 }
		
			for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productId.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
				}
			}
			clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
			for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productId.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Subscribed");
					
				}	
		}
	
	}}
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Auto bind of instance based subscription", 
			groups = { "ManuallyAttachInstanceBasedSubscriptionOnCertV1"}, enabled = true)
	public void ManuallyAttachInstanceBasedSubscriptionOnCertV1() throws JSONException,Exception {
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("system.certificate_version", "1.0");
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			if(!(installed.productId.equals("32060"))){
				moveProductCertFiles(installed.productId+"_"+ ".pem");
				moveProductCertFiles(installed.productId + ".pem");
			}
		}
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		clienttasks.facts(null, true, null, null, null);
			if(clienttasks.getFactValue("virt.is_guest").contains("false")){
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
			clienttasks.facts(null, true, null, null, null);
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
						if(availList.subscriptionName.contains("Instance Based")){
							clienttasks.subscribe(null, null, availList.poolId, null, null, "2", null, null, null, null, null);
					
			}
			}for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productId.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Subscribed");
					
				}	
			}
			String systemStatus=clienttasks.getFactValue("system.entitlements_valid");
			String expected="partial";
			Assert.assertEquals(systemStatus.trim(), expected);
			
		 }
	}	
	
	protected void moveProductCertFiles(String filename) throws IOException {
		client = new SSHCommandRunner(sm_clientHostname, sm_sshUser, sm_sshKeyPrivate,sm_sshkeyPassphrase,null);
		if(!(RemoteFileTasks.testExists(client, "/root/temp1/"))){
			client.runCommandAndWait("mkdir " + "/root/temp1/");
		}
			client.runCommandAndWait("mv " + clienttasks.productCertDir + "/"+ filename + " " + "/root/temp1/");
	
		}
	@BeforeClass(groups = "setup")
	public void rememberConfiguredHealFrequency() {
		if (clienttasks == null)
			return;
		configuredHealFrequency = Integer.valueOf(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsmcertd","autoAttachInterval"));
		configuredHostname=clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server","hostname");
	}
	
	
	@BeforeGroups(groups = "setup", value = { "BugzillaTests"}, enabled = true)
	@AfterClass(groups = "setup")
	public void restoreConfiguredHealFrequency() {
		if (clienttasks == null)
			return;
		clienttasks.restart_rhsmcertd(null, configuredHealFrequency, false,null);
	}
	
	
	@AfterGroups(groups = { "setup" }, value = { "ManuallyAttachInstanceBasedSubscriptionOnCertV1"})
	@AfterClass(groups = "setup")
	public void restoreProductCerts() throws IOException {
		client = new SSHCommandRunner(sm_clientHostname, sm_sshUser, sm_sshKeyPrivate,sm_sshkeyPassphrase,null);
		client.runCommandAndWait("mv " + "/root/temp1/*.pem" + " "
				+ clienttasks.productCertDir);
		client.runCommandAndWait("rm -rf " + "/root/temp1");
	}
	
}
	
