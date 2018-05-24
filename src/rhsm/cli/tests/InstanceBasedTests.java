package rhsm.cli.tests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

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
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36689", "RHEL7-51534"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify if instance_multiplier logic is enforced on virtual guests.", 
			groups = {"Tier2Tests","InstanceMultiplierLogicOnVirtMachines","blockedByBug-962933"},
			enabled = true)
	public void testInstanceMultiplierLogicOnVirtMachines() throws JSONException,Exception {
		Boolean flag = false;
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null, null);
		if(clienttasks.getFactValue("virt.is_guest").equals("True")){
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
				
				if(CandlepinTasks.isPoolProductInstanceBased(sm_clientUsername, sm_clientPassword, sm_serverUrl,availList.poolId)){
					flag=true;
					String quantity=availList.quantity;
					if (availList.quantity.equalsIgnoreCase("Unlimited")) quantity=String.valueOf(getRandInt());
					SSHCommandResult result=clienttasks.subscribe(null, null, availList.poolId, null, null, quantity, null, null, null, null, null, null, null);
					String expectedMessage="Successfully attached a subscription for: "+availList.subscriptionName;
					Assert.assertEquals(result.getStdout().trim(), expectedMessage);
					Assert.assertEquals(result.getExitCode(),  Integer.valueOf(0));
				}
				
				
		}if(!flag) throw new SkipException("no Instance based subscriptions are available for testing");
	
		}else 
			throw new SkipException("This test is not applicable on a Physical system.");
		
		}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36691", "RHEL7-51536"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify subscribing to Instance-Based subscriptions", 
			groups = {"Tier2Tests","SubscriptionOfInstanceBasedTest"},
			enabled = true)
	public void testSubscribingToInstanceBasedSubscriptions() throws JSONException, Exception {

		clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String) null, null, null, null, true, null, null, null, null, null);

		if (clienttasks.getFactValue("virt.is_guest").equalsIgnoreCase("false")) {
			String socket = clienttasks.getFactValue("cpu.cpu_socket(s)");
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
				if (CandlepinTasks.isPoolProductInstanceBased(sm_clientUsername, sm_clientPassword, sm_serverUrl, availList.poolId)) {
					clienttasks.subscribe(null, null, availList.poolId, null, null, socket, null, null, null, null, null, null, null);
				}

			}
			for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
				if (installed.productId.contains("Instance Server")) {
					Assert.assertEquals(installed.status.trim(), "Subscribed");
				}
			}
		}
		if (clienttasks.getFactValue("virt.is_guest").equals("True")) {
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
				if (CandlepinTasks.isPoolProductInstanceBased(sm_clientUsername, sm_clientPassword, sm_serverUrl, availList.poolId)) {
					clienttasks.subscribe(null, null, availList.poolId, null, null, null, null, null, null, null, null, null, null);
				}

			}
			for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
				if (installed.productId.contains("Instance Server")) {
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
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36688", "RHEL7-51533"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify healing of Instance-Based subscriptions", 
			groups = {"Tier2Tests","HealingOfInstanceBasedSubscription","blockedByBug-907638"},
			enabled = true)
	public void testHealingOfInstanceBasedSubscriptions() throws JSONException,Exception {
		
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, false, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		if (Boolean.valueOf(clienttasks.getFactValue("virt.is_guest"))) clienttasks.mapSystemAsAGuestOfItself();	// to avoid unmapped_guests_only pools
		clienttasks.subscribe_(true,null,(String)null,null,null,null,null,null,null,null,null,null, null);
		int healFrequency=2;
		
		if(clienttasks.getFactValue("virt.is_guest").equalsIgnoreCase("false")){
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
				if(CandlepinTasks.isPoolProductInstanceBased(sm_clientUsername, sm_clientPassword, sm_serverUrl,availList.poolId)){
					clienttasks.subscribe(null, null, availList.poolId, null, null, "2", null, null, null, null, null, null, null);

				}
			}
			String messageDetails="Only covers 2 of "+sockets+" sockets.";

			for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productId.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
					Assert.assertEquals(installed.statusDetails, messageDetails);
				}
			}
		}
/* replacing call to restart_rhsmcertd with a faster call to run_rhsmcertd_worker	
		clienttasks.restart_rhsmcertd(null, healFrequency, true);
		SubscriptionManagerCLITestScript.sleep(healFrequency * 60 * 1000);
*/		clienttasks.run_rhsmcertd_worker(true);		
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productName.contains("Instance Server")){
				Assert.assertEquals(installed.status.trim(), "Subscribed");
			}
		}
		if(clienttasks.getFactValue("virt.is_guest").equals("True")){
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
/* replacing call to restart_rhsmcertd with a faster call to run_rhsmcertd_worker	
			clienttasks.restart_rhsmcertd(null, healFrequency, true);
			SubscriptionManagerCLITestScript.sleep(healFrequency * 60 * 1000);
*/			clienttasks.run_rhsmcertd_worker(true);		
			for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
				if(installed.productName.contains("Instance Server")){
					Assert.assertEquals(installed.status.trim(), "Subscribed");
				}
			}
			for(ProductSubscription consumed:clienttasks.getCurrentlyConsumedProductSubscriptions()){
				if(consumed.productName.contains("Instance Based")){
					Integer quantity =1;		
					Assert.assertEquals(consumed.quantityUsed,quantity );
				}
			}
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36687", "RHEL7-51532"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify auto subscribing of Instance-Based subscriptions", 
			groups = {"Tier2Tests","AutoSubscribingInstanceBasedSubscription"},
			enabled = true)
	public void testAutoSubscribingInstanceBasedSubscriptions() throws JSONException, Exception {

		clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, false, null, null, (String) null, null, null, null, true, null, null, null, null, null);
		if (Boolean.valueOf(clienttasks.getFactValue("virt.is_guest"))) clienttasks.mapSystemAsAGuestOfItself(); // to avoid unmapped_guests_only pools
		clienttasks.subscribe_(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		if (clienttasks.getFactValue("virt.is_guest").equalsIgnoreCase("false")) {
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
			clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null,
					null);
			for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
				if (installed.productName.contains("Instance Server")) {
					Assert.assertEquals(installed.status.trim(), "Subscribed");
				}
			}
			for (ProductSubscription consumed : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
				if (consumed.productName.contains("Instance Based")) {
					String SocketsCount = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumed.poolId, "sockets");
					sockets = sockets / Integer.parseInt(SocketsCount);
					Assert.assertEquals(consumed.quantityUsed, sockets);
				}
			}

		}
		if (clienttasks.getFactValue("virt.is_guest").equals("True")) {
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
			clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
			for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
				if (installed.productName.contains("Instance Server")) {
					Assert.assertEquals(installed.status.trim(), "Subscribed");
				}
			}
			for (ProductSubscription consumed : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
				if (consumed.productName.contains("Instance Based")) {
					Integer quantity = 1;
					Assert.assertEquals(consumed.quantityUsed, quantity);
				}
			}
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36690", "RHEL7-51535"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify stacking of Instance-Based subscriptions", 
			groups = {"Tier2Tests","StackingOfInstanceBasedSubscription"},
			enabled = true)
	public void testStackingOfInstanceBasedSubscriptions() throws JSONException, Exception {

		clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String) null, null, null, null, true, null, null, null, null, null);

		if (clienttasks.getFactValue("virt.is_guest").equalsIgnoreCase("false")) {
			Integer sockets = 4;
			String poolId = null;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
				if (availList.subscriptionName.contains("Instance Based")) {
					poolId = availList.poolId;
					clienttasks.subscribe(null, null, availList.poolId, null, null, "2", null, null, null, null, null, null, null);
				}
			}
			String messageDetails = "Only covers 2 of " + sockets + " sockets.";

			for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
				if (installed.productId.contains("Instance Server")) {
					Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
					Assert.assertEquals(installed.statusDetails, messageDetails);
				}
			}
			clienttasks.subscribe(null, null, poolId, null, null, "2", null, null, null, null, null, null, null);
			for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
				if (installed.productId.contains("Instance Server")) {
					Assert.assertEquals(installed.status.trim(), "Subscribed");
				}
			}
		}
		if (clienttasks.getFactValue("virt.is_guest").equals("True")) {
			Integer sockets = 4;
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
			for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
				if (availList.subscriptionName.contains("Instance Based")) {
					clienttasks.subscribe(null, null, availList.poolId, null, null, "1", null, null, null, null, null, null, null);
				}
			}

			for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
				if (installed.productId.contains("Instance Server")) {
					Assert.assertEquals(installed.status.trim(), "Subscribed");
				}
			}
		}
	}
	

	
	protected void moveProductCertFiles(String filename) throws IOException {
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
	
/*	this will run restoreConfiguredHealFrequency() before the BugzillaTests class.  This is not what you want to do
	@BeforeGroups(groups = "setup", value = { "BugzillaTests"}, enabled = true)
*/
	@AfterClass(groups = "setup")
	public void restoreConfiguredHealFrequency() {
		if (clienttasks == null)
			return;
		clienttasks.restart_rhsmcertd(null, configuredHealFrequency, true);
	}
	
	
	@AfterGroups(groups = { "setup" }, value = { "ManuallyAttachInstanceBasedSubscriptionOnCertV1"})
	@AfterClass(groups = "setup")
	public void restoreProductCerts() throws IOException {
		client.runCommandAndWait("mv " + "/root/temp1/*.pem" + " "
				+ clienttasks.productCertDir);
		client.runCommandAndWait("rm -rf " + "/root/temp1");
	}
	
}
	
