package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.jul.TestRecords;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author skallesh
 * 
 * Beware of "Bug 1439201 - udev-kvm-check and brandbot needs a new home" which may eliminate brandbot on RHEL8
 * 
 */
@Test(groups = {"FlexibleBrandingTests"})
public class FlexibleBrandingTests extends SubscriptionManagerCLITestScript {
	protected  static String BrandType = null;
	protected String ownerKey="";
	protected String randomAvailableProductId;
	protected EntitlementCert expiringCert = null;
	protected String EndingDate;
	protected final String importCertificatesDir = "/tmp/sm-importExpiredCertificatesDir"
			.toLowerCase();
	List<String> providedProducts = new ArrayList<String>();
	protected List<File> entitlementCertFiles = new ArrayList<File>();
	protected final String Brand_Name = "/var/lib/rhsm/branded_name".toLowerCase();


	
		
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36693", "RHEL7-51539"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if brandname file is created",
			groups={"Tier2Tests","VerifyBrandFileCreation"},
			enabled=true)
	public void testBrandNameFileCreation() throws Exception {
		// reset the installed product cert to productCert32060 and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);

		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null, null, null);
		List<ProductSubscription> consumed=clienttasks.getCurrentlyConsumedProductSubscriptions();
		String productname="Branded "+consumed.get(randomGenerator.nextInt(consumed.size())).productName;
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36698", "RHEL7-51544"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if brandname file is deleted",
			groups={"Tier2Tests","VerifyBrandFileDeletion"},
			enabled=true)
	public void testBrandNameFileNotDeletedAfterUnsubscribing() throws Exception {
		// reset the installed product cert to productCert32060 and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
		
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);

		
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null, null, null);
		List<ProductSubscription> consumed=clienttasks.getCurrentlyConsumedProductSubscriptions();
		String productname="Branded "+consumed.get(randomGenerator.nextInt(consumed.size())).productName;
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null, null, null);
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36701", "RHEL7-51547"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if brandname file are replaced",
			groups={"Tier2Tests","VerifyBrandFileContents"},
			enabled=true)
	public void testBrandNameContentsAreReplaced() throws Exception {
		// reset the installed product cert to productCert32060 and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
		

		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null, null, null);
		List<ProductSubscription> consumed=clienttasks.getCurrentlyConsumedProductSubscriptions();
		String productname="Branded "+consumed.get(randomGenerator.nextInt(consumed.size())).productName;

		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null, null, null);
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null, null, null);
		result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null, null, null);
		Assert.assertEquals(result.trim(), productname.trim());
	
		
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36700", "RHEL7-51546"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if brandtype value is present in the entitlement cert",
			groups={"Tier2Tests","VerifyBrand_TypeValue"},
			enabled=true) 
	public void testBrandTypeValue() throws Exception {
		// reset the installed product cert to productCert32060 and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());

		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);

		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null, null, null);
		List<ProductSubscription> consumed=clienttasks.getCurrentlyConsumedProductSubscriptions();
		String productname="Branded "+consumed.get(randomGenerator.nextInt(consumed.size())).productName;
		client.runCommandAndWaitWithoutLogging("find "+clienttasks.entitlementCertDir+" -regex \"/.+/[0-9]+.pem\" -exec rct cat-cert {} \\;");
		String certificates = client.getStdout();
		String BrandType=parseInfo(certificates);
			Assert.assertEquals(BrandType, "OS");
		}
	
	
	

	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36695", "RHEL7-51541"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if brandname file is created for imported cert",
			groups={"Tier2Tests","CreationWithImport"},
			enabled=true) 
	public void testBrandNameFileCreationWithImportedCert() throws Exception {
		// reset the installed product cert to productCert32060 and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());


		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);

		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null, null, null);
		List<ProductSubscription> consumed=clienttasks.getCurrentlyConsumedProductSubscriptions();
		String productname="Branded "+consumed.get(randomGenerator.nextInt(consumed.size())).productName;
		client.runCommandAndWait("mkdir /root/importedcertDir");
		client.runCommandAndWait("cat "+clienttasks.entitlementCertDir+"/* >> /root/importedcertDir/importedcert.pem");
		client.runCommandAndWait("rm -rf "+Brand_Name);
		clienttasks.clean();
		clienttasks.importCertificate("/root/importedcertDir/importedcert.pem");
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		client.runCommandAndWait("rm -rf /root/importedcertDir/importedcert.pem");
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null, null, null);
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36696", "RHEL7-51542"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if brandname file is created when rhsmcertd service runs",
			groups={"Tier2Tests","CreationWithRHSMCERTD","blockedByBug-907638"},
			enabled=true) 
	public void testBrandNameFileCreationWithRhsmcertd() throws Exception {
		// reset the installed product cert to productCert32060 and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
			clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, null, null, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		List<ProductSubscription> consumed=clienttasks.getCurrentlyConsumedProductSubscriptions();
		String productname="Branded "+consumed.get(randomGenerator.nextInt(consumed.size())).productName;
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null, null, null);
		result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36697", "RHEL7-51543"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if brandname file is created when two RHEL products are installed",
			groups={"Tier2Tests","CreationWithTwoRhelproducts"},
			enabled=true) 
	public void testBrandNameFileCreationWithTwoRhelProductInstalled() throws Exception {
		clienttasks.unregister(null, null, null, null);
		
		// reset the installed product certs and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert37060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		String stdoutResult=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		if(stdoutResult.contains("Branded")) {
			RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
		}
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, null, false, null, null, null, null);
		
		//	[root@rhel6 ~]# tail -f /var/log/rhsm/rhsm.log
		//	[DEBUG] subscription-manager:29170:MainThread @certdirectory.py:217 - Installed product IDs: ['69', '32060', '37060']
		//	[DEBUG] subscription-manager:29170:MainThread @rhelentbranding.py:121 - Installed branded product: <rhsm.certificate2.Product object at 0x15910d0>
		//	[DEBUG] subscription-manager:29170:MainThread @rhelentbranding.py:121 - Installed branded product: <rhsm.certificate2.Product object at 0x1591a10>
		//	[DEBUG] subscription-manager:29170:MainThread @rhelentbranding.py:124 - 2 entitlement certs with brand info found
		//	[WARNING] subscription-manager:29170:MainThread @rhelentbranding.py:95 - More than one entitlement provided branded name information for an installed RHEL product
		String result=client.runCommandAndWait("cat "+Brand_Name).getStderr();

		String expectedMessage="cat: /var/lib/rhsm/branded_name: No such file or directory";
		Assert.assertEquals(result.trim(), expectedMessage);
	}
	
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36694", "RHEL7-51540"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if brandname file is created when registering with an activation key",
			groups={"Tier2Tests","CreationWithTActivationKey"},
			enabled=true) 
	public void testBrandNameFileCreationWithActivationKeys() throws Exception {
		// reset the installed product cert to productCert32060 and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
		
		Integer addQuantity=null;
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null, null);
		if(clienttasks.getFactValue("virt.is_guest").equals("True")){
			addQuantity=1;
		}else{
			addQuantity=2;
		}
		String name = String.format("%s_%s-ActivationKey%s", sm_clientUsername,
				sm_clientOrg, System.currentTimeMillis());
		Map<String, String> mapActivationKeyRequest = new HashMap<String, String>();
		mapActivationKeyRequest.put("name", name);
		JSONObject jsonActivationKeyRequest = new JSONObject(
				mapActivationKeyRequest);
		JSONObject jsonActivationKey = new JSONObject(
				CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername,
						sm_clientPassword, sm_serverUrl, "/owners/"
								+ sm_clientOrg + "/activation_keys",
								jsonActivationKeyRequest.toString()));
		String poolId=null;

		for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
					if(availList.subscriptionName.contains("Instance")){
						poolId=availList.poolId;
			
		}
		}

		new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/activation_keys/" + jsonActivationKey.getString("id") + "/pools/" +poolId+(addQuantity==null?"":"?quantity="+addQuantity), null));
		
		clienttasks.register(null, null, sm_clientOrg, null, null, null, null, null, null, null, name, null, null, null, true, null, null, null, null, null);
		List<ProductSubscription> consumed=clienttasks.getCurrentlyConsumedProductSubscriptions();
		String productname="Branded "+consumed.get(randomGenerator.nextInt(consumed.size())).productName;
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36699", "RHEL7-51545"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if brandname file is created without product cert",
			groups={"Tier2Tests","verifyFile"},
			enabled=true)
	public void testBrandNameFileShouldNotBeCreatedWithoutProduct() throws Exception {
		// reset the installed product certs and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
		
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		

		clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);
		for(SubscriptionPool pool:clienttasks.getCurrentlyAllAvailableSubscriptionPools()){
			if(pool.subscriptionName.contains("Instance")){
				clienttasks.subscribeToSubscriptionPool(pool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl);
			}
		}
		String result=client.runCommandAndWait("cat "+Brand_Name).getStderr();
		String expectedMessage="cat: /var/lib/rhsm/branded_name: No such file or directory";
		Assert.assertEquals(result.trim(), expectedMessage);
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36692", "RHEL7-51538"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify if Brand Name Creation During Repeated RHSMCERTD Updates",
			groups={"Tier2Tests","verifyFileduringrhsmupdates","blockedByBug-907638"},
			enabled=true)
	public void testBrandNameCreatedDuringRepeatedRhsmcertdUpdates() throws Exception {
		// reset the installed product cert to productCert32060 and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
	
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);

		for (int i=1; i <= 2; i++) {	// Repeated RHSMCERTD Updates
			RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
			clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null, null, null);
			List<ProductSubscription> consumed=clienttasks.getCurrentlyConsumedProductSubscriptions();
			String productname="Branded "+consumed.get(randomGenerator.nextInt(consumed.size())).productName;
			String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
			Assert.assertEquals(result.trim(), productname.trim());
			
			clienttasks.unsubscribe(true,(BigInteger)null, null, null, null, null, null);
			result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
			Assert.assertEquals(result.trim(), productname.trim());
		}
	}
	
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="verify if brandname file is created cert version is 1",
			groups={"Tier2Tests","VerifyBrand_NameCreatedWithCertV1","blockedByBug-1011768"},
			enabled=false)//bug has been closed as not a bug
	public void testBrandNameCreatedWithCertV1() throws Exception {
		// reset the installed product cert to productCert32060 and reset the brand file
		clienttasks.removeAllCerts(false, false, true);
		RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert32060.file+" "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+Brand_Name, TestRecords.action());
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("system.certificate_version", String.valueOf(1));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null, null, null);
		List<ProductSubscription> consumed=clienttasks.getCurrentlyConsumedProductSubscriptions();
		String productname="Branded "+consumed.get(randomGenerator.nextInt(consumed.size())).productName;
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		clienttasks.deleteFactsFileWithOverridingValues();
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	

	static public String parseInfo(String rawCertificates) {
		Map<String,String> regexes = new HashMap<String,String>();
		List certData = new ArrayList();
		regexes.put("BrandType",			"Product:(?:(?:\\n.+)+)Brand Type: (.+)");
		// split the rawCertificates process each individual rawCertificate
		String rawCertificateRegex = "\\+-+\\+\\n\\s+Entitlement Certificate\\n\\+-+\\+";
		for (String rawCertificate : rawCertificates.split(rawCertificateRegex)) {
			
			// strip leading and trailing blank lines and skip blank rawCertificates
			rawCertificate = rawCertificate.replaceAll("^\\n*","").replaceAll("\\n*$", "");
			if (rawCertificate.length()==0) continue;
			List<Map<String,String>> certDataList = new ArrayList<Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			//	BrandTypeValue=regexes.get(field);
				addRegexMatchesToList(pat, rawCertificate, certDataList, field);
			}
			
			// assert that there is only one group of certData found in the list
			List<String> BrandTypeValue = new ArrayList<String>();
			for(Map<String,String> CertMap : certDataList) {
				// normalize newlines from productName when it spans multiple lines (introduced by bug 864177)
				String key = "BrandType",Brandtype = CertMap.get(key);
				if (Brandtype!=null) {
					CertMap.remove(key);
					BrandType = Brandtype.replaceAll("\\s*\\n\\s*", " ");
				}
				//BrandTypeValue.add(BrandType);
			}}
			return BrandType;
			
	}
		
		
	static protected boolean addRegexMatchesToList(Pattern regex, String to_parse, List<Map<String,String>> matchList, String sub_key) {
		boolean foundMatches = false;
		Matcher matcher = regex.matcher(to_parse);
		int currListElem=0;
		while (matcher.find()){
			if (matchList.size() < currListElem + 1) matchList.add(new HashMap<String,String>());
			Map<String,String> matchMap = matchList.get(currListElem);
			matchMap.put(sub_key, matcher.group(1).trim());
			matchList.set(currListElem, matchMap);
			currListElem++;
			foundMatches = true;
		}
        if (!foundMatches) {
        	//log.warning("Could not find regex '"+regex+"' match for field '"+sub_key+"' while parsing: "+to_parse );
        	log.finer("Could not find regex '"+regex+"' match for field '"+sub_key+"' while parsing: "+to_parse );
        }
		return foundMatches;
	}
	
	
	
	
	
	
	
	ProductCert productCert32060 = null;
	ProductCert productCert37060 = null;
	protected String originalProductCertDir		= null;
	protected final String tmpProductCertDir	= "/tmp/sm-fbProductCertDir";

	@BeforeClass(groups="setup")
	public void configProductCertDirBeforeClass() {
		if (clienttasks==null) return;
		
		List<ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		productCert32060 = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "32060", productCerts);
		productCert37060 = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "37060", productCerts);
		if (productCert32060==null) throw new SkipException("Could not find expected flexible branded product cert id 32060 installed.");
		if (productCert37060==null) throw new SkipException("Could not find expected flexible branded product cert id 37060 installed.");
		
		// TEMPORARY WORKAROUND FOR BUG: Bug 1183175 - changing to a different rhsm.productcertdir configuration throws OSError: [Errno 17] File exists
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1183175"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			throw new SkipException("Cannot configure a different productCertDir while bug '"+bugId+"' is open.");
		}
		// END OF WORKAROUND
		
		originalProductCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
		Assert.assertNotNull(originalProductCertDir);
		log.info("Initializing a new product cert directory containing product 32060...");
		RemoteFileTasks.runCommandAndAssert(client,"mkdir -p "+tmpProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"rm -f "+tmpProductCertDir+"/*.pem",Integer.valueOf(0));
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", tmpProductCertDir);
	}
	
	@AfterClass(groups="setup")
	public void unconfigProductCertDirAfterClass() {
		if (clienttasks==null) return;
		if (originalProductCertDir==null) return;	
		log.info("Restoring the originally configured product cert directory...");
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", originalProductCertDir);
	}
	
}

