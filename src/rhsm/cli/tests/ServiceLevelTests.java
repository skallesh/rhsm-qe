package rhsm.cli.tests;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.ProductNamespace;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 *
 * Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe
 */

@Test(groups={"ServiceLevelTests"})
public class ServiceLevelTests extends SubscriptionManagerCLITestScript {

		
	// Test methods ***********************************************************************


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21874", "RHEL7-51713"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level (when not registered)",
			groups={"Tier3Tests","blockedByBug-826856"/*,"blockedByBug-837036"*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelWhenNotRegistered() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		SSHCommandResult result;
		
		// with credentials
		result = clienttasks.service_level_(null, null, null, null, sm_clientUsername, sm_clientPassword, /*sm_clientOrg*/null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from service-level (implies --show) without being registered");
			Assert.assertEquals(result.getStderr().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stderr from service-level --show without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), new Integer(255), "ExitCode from service-level (implies --show) without being registered");
			//Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
			Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from service-level --show without being registered");
		}
		if (sm_clientOrg!=null) {
			result = clienttasks.service_level_(null, null, null, null, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null);
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				Assert.assertEquals(result.getExitCode(), Integer.valueOf(64)/*EX_USAGE*/, "ExitCode from service-level (implies --show) without being registered");
				Assert.assertEquals(result.getStderr().trim(),"Error: --org is only supported with the --list option", "Stderr from service-level --show without being registered");
			} else {
				Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level (implies --show) without being registered");
				Assert.assertEquals(result.getStdout().trim(),"Error: --org is only supported with the --list option", "Stdout from service-level --show without being registered");
			}
		}
		
		// without credentials
		result = clienttasks.service_level_(null, null, null, null, null, null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from service-level (implies --show) without being registered");
			Assert.assertEquals(result.getStderr().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stderr from service-level --show without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), new Integer(255), "ExitCode from service-level (implies --show) without being registered");
			//Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
			Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from service-level --show without being registered");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21878", "RHEL7-51717"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --show (when not registered)",
			groups={"Tier3Tests","blockedByBug-826856",/*"blockedByBug-837036"*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelShowWhenNotRegistered() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		SSHCommandResult result;
		
		// with credentials
		result = clienttasks.service_level_(true, null, null, null, sm_clientUsername, sm_clientPassword, /*sm_clientOrg*/null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "ExitCode from service-level --show without being registered");
			Assert.assertEquals(result.getStderr().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stderr from service-level --show without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --show without being registered");
			//Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
			Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from service-level --show without being registered");
		}
		if (sm_clientOrg!=null) {
			result = clienttasks.service_level_(null, null, null, null, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null);
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				Assert.assertEquals(result.getExitCode(), Integer.valueOf(64)/*EX_USAGE*/, "ExitCode from service-level --show without being registered");
				Assert.assertEquals(result.getStderr().trim(),"Error: --org is only supported with the --list option", "Stderr from service-level --show without being registered");
			} else {
				Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --show without being registered");
				Assert.assertEquals(result.getStdout().trim(),"Error: --org is only supported with the --list option", "Stdout from service-level --show without being registered");
			}
		}
		
		// without credentials
		result = clienttasks.service_level_(true, null, null, null, null, null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "ExitCode from service-level --show without being registered");
			Assert.assertEquals(result.getStderr().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stderr from service-level --show without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --show without being registered");
			//Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
			Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from service-level --show without being registered");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21871", "RHEL7-51722"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --list (when not registered)",
			groups={"Tier3Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelListWhenNotRegistered() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		SSHCommandResult result = clienttasks.service_level_(null, true, null, null, null, null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(64)/*EX_USAGE*/, "ExitCode from service-level --list without being registered");
			Assert.assertEquals(result.getStderr().trim(),"Error: you must register or specify --username and --password to list service levels", "Stderr from service-level --list without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list without being registered");
			Assert.assertEquals(result.getStdout().trim(),"Error: you must register or specify --username and --password to list service levels", "Stdout from service-level --list without being registered");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21872", "RHEL7-51726"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --set (when not registered)",
			groups={"Tier3Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelSetWhenNotRegistered() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		SSHCommandResult result;
		
		// with credentials
		result = clienttasks.service_level_(null, null, "FOO", null, sm_clientUsername, sm_clientPassword, /*sm_clientOrg*/null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from service-level --set without being registered");
			Assert.assertEquals(result.getStderr().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stderr from service-level --set without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), new Integer(255), "ExitCode from service-level --set without being registered");
			//Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --set without being registered");
			Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from service-level --set without being registered");
		}
		if (sm_clientOrg!=null) {
			result = clienttasks.service_level_(null, null, "FOO", null, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null);
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				Assert.assertEquals(result.getExitCode(), Integer.valueOf(64)/*EX_USAGE*/, "ExitCode from service-level --set without being registered");
				Assert.assertEquals(result.getStderr().trim(),"Error: --org is only supported with the --list option", "Stderr from service-level --set without being registered");
			} else {
				Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --set without being registered");
				Assert.assertEquals(result.getStdout().trim(),"Error: --org is only supported with the --list option", "Stdout from service-level --set without being registered");
			}
		}
		
		// without credentials
		result = clienttasks.service_level_(null, null, "FOO", null, null, null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from service-level --set without being registered");
			Assert.assertEquals(result.getStderr().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stderr from service-level --set without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), new Integer(255), "ExitCode from service-level --set without being registered");
			//Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --set without being registered");
			Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from service-level --set without being registered");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21868", "RHEL7-51727"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --unset (when not registered)",
			groups={"Tier3Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelUnsetWhenNotRegistered() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		SSHCommandResult result;
		
		// with credentials
		result = clienttasks.service_level_(null, null, null, true, sm_clientUsername, sm_clientPassword, /*sm_clientOrg*/null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from service-level --unset without being registered");
			Assert.assertEquals(result.getStderr().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stderr from service-level --unset without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), new Integer(255), "ExitCode from service-level --unset without being registered");
			//Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --unset without being registered");
			Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from service-level --unset without being registered");
		}
		if (sm_clientOrg!=null) {
			result = clienttasks.service_level_(null, null, null, true, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null);
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				Assert.assertEquals(result.getExitCode(), Integer.valueOf(64)/*EX_USAGE*/, "ExitCode from service-level --unset without being registered");
				Assert.assertEquals(result.getStderr().trim(),"Error: --org is only supported with the --list option", "Stderr from service-level --unset without being registered");
			} else {
				Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --unset without being registered");
				Assert.assertEquals(result.getStdout().trim(),"Error: --org is only supported with the --list option", "Stdout from service-level --unset without being registered");
			}
		}
		
		// without credentials
		result = clienttasks.service_level_(null, null, null, true, null, null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from service-level --unset without being registered");
			Assert.assertEquals(result.getStderr().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stderr from service-level --unset without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), new Integer(255), "ExitCode from service-level --unset without being registered");
			//Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --unset without being registered");
			Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from service-level --unset without being registered");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21877", "RHEL7-51729"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --list (with invalid credentials)",
			groups={"Tier3Tests","blockedByBug-1256960"/*is a duplicate of*/,"blockedByBug-1254349"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelListWithInvalidCredentials() {
		String x = String.valueOf(getRandInt());
		SSHCommandResult result;
				
		// test while unregistered
		clienttasks.unregister(null, null, null, null);
		result = clienttasks.service_level_(null, true, null, null, sm_clientUsername, sm_clientPassword+x, sm_clientOrg, null, null, null, null, null, null);
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "ExitCode from service-level --list with invalid credentials");
		//if (sm_serverOld) {Assert.assertEquals(result.getStdout().trim(), "Error: you must register or specify --org."); throw new SkipException("service-level --list with invalid credentials against an old candlepin server is not supported.");}
		if (sm_serverOld) {Assert.assertEquals(result.getStderr().trim(), "ERROR: The service-level command is not supported by the server."); throw new SkipException("Skipping this test since service-level is gracefully not supported when configured against an old candlepin server.");}
		Assert.assertEquals(result.getStderr().trim(), servertasks.invalidCredentialsMsg(), "Stderr from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");

		// test while registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,null, null, null, null, null, null);
		result = clienttasks.service_level_(null, true, null, null, sm_clientUsername, sm_clientPassword+x, sm_clientOrg, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "ExitCode from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStderr().trim(), servertasks.invalidCredentialsMsg(), "Stderr from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21865", "RHEL7-51721"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --list (with invalid org)",
			groups={"Tier3Tests","blockedByBug-796468","blockedByBug-815479","blockedByBug-1256960"/*is a duplicate of*/,"blockedByBug-1254349"},
			enabled=true)
	@ImplementsNitrateTest(caseId=165509)
	public void testServiceLevelListWithInvalidOrg() {
		String x = String.valueOf(getRandInt());
		SSHCommandResult result;
				
		// test while unregistered
		clienttasks.unregister(null, null, null, null);
		result = clienttasks.service_level_(null, true, null, null, sm_clientUsername, sm_clientPassword, sm_clientOrg+x, null, null, null, null, null, null);
		if (sm_serverOld) {Assert.assertEquals(result.getStderr().trim(), "ERROR: The service-level command is not supported by the server."); throw new SkipException("Skipping this test since service-level is gracefully not supported when configured against an old candlepin server.");}
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "ExitCode from service-level --list with invalid org");
		Assert.assertEquals(result.getStderr().trim(), String.format("Organization with id %s could not be found.",sm_clientOrg+x), "Stderr from service-level --list with invalid org");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");

		// test while registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,null, null, null, null, null, null);
		result = clienttasks.service_level_(null, true, null, null, sm_clientUsername, sm_clientPassword, sm_clientOrg+x, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "ExitCode from service-level --list with invalid org");
		Assert.assertEquals(result.getStderr().trim(), String.format("Organization with id %s could not be found.",sm_clientOrg+x), "Stderr from service-level --list with invalid org");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");
	}
	
	
	@Test(	description="subscription-manager: service-level --show (after registering without a service level)",
			groups={"Tier3Tests"},
			enabled=false) // assertions in this test are already a subset of ServiceLevelShowAvailable_Test(...)
	@Deprecated
	@ImplementsNitrateTest(caseId=157213)
	public void testServiceLevelShowAfterRegisteringWithoutServiceLevel_DEPRECATED() throws JSONException, Exception  {
		SSHCommandResult result;
				
		// register with no service-level
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null));
		
		// get the current consumer object and assert that the serviceLevel persisted
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertEquals(jsonConsumer.get("serviceLevel"), JSONObject.NULL, "The call to register without a servicelevel leaves the current consumer object serviceLevel attribute value null.");
		
		result = clienttasks.service_level(true, false, null, null, null, null, null, null, null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Current service level:", "When the system has been registered without a service level, the current service level should be null.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19993", "RHEL7-51025"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: service-level --show (after registering without a service level)",
			groups={"Tier1Tests"},
			dataProvider="getRegisterCredentialsExcludingNullOrgData",
			enabled=true)
	@ImplementsNitrateTest(caseId=155949)
	public void testServiceLevelShowAvailable(String username, String password, String org) throws JSONException, Exception  {
				
		// register with no service-level
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(username,password,org,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null));
		
		// get the current consumer object and assert that the serviceLevel is empty (value is "")
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(username,password,sm_serverUrl,"/consumers/"+consumerId));
		// Assert.assertEquals(jsonConsumer.get("serviceLevel"), JSONObject.NULL, "The call to register without a servicelevel leaves the current consumer object serviceLevel attribute value null.");	// original value was null
		if (sm_serverOld) {Assert.assertFalse(jsonConsumer.has("serviceLevel"), "Consumer attribute serviceLevel should not exist against an old candlepin server."); throw new SkipException("The service-level command is not supported by the server.");}
		Assert.assertEquals(jsonConsumer.get("serviceLevel"), "", "The call to register without a servicelevel leaves the current consumer object serviceLevel attribute value empty.");
	
		// assert that "Current service level:" is empty
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), "", "When the system has been registered without a service level, the current service level value should be empty.");
		//Assert.assertEquals(clienttasks.service_level(null,null,null,null,null,null,null,null, null).getStdout().trim(), "Current service level:", "When the system has been registered without a service level, the current service level value should be empty.");
		Assert.assertEquals(clienttasks.service_level(null,null,null,null,null,null,null,null, null, null, null, null, null).getStdout().trim(), "Service level preference not set", "When the system has been registered without a service level, the current service level value should be empty.");

		// get all the valid service levels available to this org	
		List<String> serviceLevelsExpected = CandlepinTasks.getServiceLevelsForOrgKey(/*sm_serverAdminUsername*/username, /*sm_serverAdminPassword*/password, sm_serverUrl, org);
		
		// assert that all the valid service levels are returned by service-level --list
		List<String> serviceLevelsActual = clienttasks.getCurrentlyAvailableServiceLevels();		
		Assert.assertTrue(serviceLevelsExpected.containsAll(serviceLevelsActual)&&serviceLevelsActual.containsAll(serviceLevelsExpected), "The actual service levels available to the current consumer "+serviceLevelsActual+" match the expected list of service levels available to the org '"+org+"' "+serviceLevelsExpected+".");

		// assert that exempt service levels do NOT appear as valid service levels
		for (String sm_exemptServiceLevel : sm_exemptServiceLevelsInUpperCase) {
			for (String serviceLevel : serviceLevelsExpected) {
				
				// TEMPORARY WORKAROUND FOR BUG - product attributes with support_level=Layered also need support_level_exempt=true
				if (serviceLevel.equalsIgnoreCase(sm_exemptServiceLevel) && sm_serverType.equals(CandlepinType.hosted)) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="840022"; 
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						throw new SkipException("Skipping this test with serviceLevel='"+serviceLevel+"' against a hosted candlepin while bug "+bugId+" is open.");
					}
					invokeWorkaroundWhileBugIsOpen = true;
					bugId="1069291"; 
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						throw new SkipException("Skipping this test with serviceLevel='"+serviceLevel+"' against a hosted candlepin while bug "+bugId+" is open.");
					}
				}
				// END OF WORKAROUND

				Assert.assertTrue(!serviceLevel.equalsIgnoreCase(sm_exemptServiceLevel), "Regardless of case, available service level '"+serviceLevel+"' should NOT match exempt service level '"+sm_exemptServiceLevel+"'.");
			}
		}
		
		// subscribe with each service level and assert that it persists as the system preference
		String currentServiceLevel = clienttasks.getCurrentServiceLevel();
		for (String serviceLevel : serviceLevelsExpected) {
			
			// the following if block was added after implementation of Bug 864207 - 'subscription-manager subscribe --auto' should be smart enough to not run when all products are subscribed already
			// when the system is already valid (all of the currently installe products are subscribed), then auto-subscribe will do nothing and the service level will remain
			if (clienttasks.getFactValue("system.entitlements_valid").equalsIgnoreCase("valid")) {
				// verify that auto-subscribe will throw a blocker message and the current service level will remain
				SSHCommandResult result = clienttasks.subscribe_(true, serviceLevel, (String)null, (String)null, (String)null, null, null, null, null, null, null, null, null);
				String expectedStdout = "All installed products are covered by valid entitlements. No need to update subscriptions at this time.";
				Assert.assertTrue(result.getStdout().trim().startsWith(expectedStdout), "When the system is already compliant, an attempt to auto-subscribe should inform us with exactly this message: "+expectedStdout);
				Assert.assertEquals(clienttasks.getCurrentServiceLevel(), currentServiceLevel, "When the system is already compliant, an attempt to auto-subscribe with a servicelevel should NOT alter the current service level: "+currentServiceLevel);
				clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);
			}
			
			// auto-subscribe and assert that the requested service level is persisted
			clienttasks.subscribe_(true, serviceLevel, (String)null, (String)null, (String)null, null, null, null, null, null, null, null, null);
			currentServiceLevel = clienttasks.getCurrentServiceLevel();
			Assert.assertEquals(currentServiceLevel, serviceLevel, "When the system is auto subscribed with the service level option, the service level should be persisted as the new preference.");
		}
	}





	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21863", "RHEL7-51725"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: register to a Candlepin server using autosubscribe with an unavailable servicelevel",
			groups={"Tier3Tests","blockedByBug-795798","blockedByBug-864508","blockedByBug-864508","blockedByBug-1221273"},
			enabled=true)
	public void testRegisterWithUnavailableServiceLevel() {

		// attempt the registration
		String unavailableServiceLevel = "FOO";
		SSHCommandResult sshCommandResult = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, unavailableServiceLevel, null, (String)null, null, null, null, true, null, null, null, null, null);
		String msg;
		msg = "Cannot set a service level for a consumer that is not available to its organization."; // valid before bug fix 795798 - Cannot set a service level for a consumer that is not available to its organization.
		msg = String.format("Service level %s is not available to consumers of organization %s.",unavailableServiceLevel,sm_clientOrg);	// valid before bug fix 864508 - Service level {0} is not available to consumers....
		msg = String.format("Service level '%s' is not available to consumers of organization %s.",unavailableServiceLevel,sm_clientOrg);
		if (!clienttasks.workaroundForBug876764(sm_serverType)) msg = String.format("Service level '%s' is not available to units of organization %s.",unavailableServiceLevel,sm_clientOrg);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			msg = String.format("Service level \"%s\" is not available to units of organization %s.",unavailableServiceLevel,sm_clientOrg);
		}
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.15.9-5")) expectedExitCode = new Integer(1);	// post RHEL7.2 commit 84340a0acda9f070e3e0b733e4335059b5dc204e bug 1221273
		Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode);
 		if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.3-1")) {	// post commit 7795df84edcb4f4fef08085548f6c2a23f86ceb4 bug 1262919: Added convenience function for printing to stderr
			Assert.assertEquals(sshCommandResult.getStderr().trim(), msg, "Stderr message from an attempt to register with autosubscribe and an unavailable servicelevel.");
 		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.15.9-5")) {	// post RHEL7.2 commit 84340a0acda9f070e3e0b733e4335059b5dc204e bug 1221273
			Assert.assertTrue(sshCommandResult.getStdout().trim().contains(msg), "Stdout message contains: "+msg);
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr message from an attempt to register with autosubscribe and an unavailable servicelevel.");
 		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(sshCommandResult.getStderr().trim(), msg, "Stderr message from an attempt to register with autosubscribe and an unavailable servicelevel.");
		} else {
			Assert.assertTrue(sshCommandResult.getStdout().trim().contains(msg), "Stdout message contains: "+msg);
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr message from an attempt to register with autosubscribe and an unavailable servicelevel.");
		}
		
		// despite a failed attempt to set a service level, we should still be registered
		Assert.assertNotNull(clienttasks.getCurrentConsumerId(), "Despite a failed attempt to set a service level during register with autosubscribe, we should still be registered");
		
		// since the autosubscribe was aborted, we should not be consuming and entitlements
		Assert.assertTrue(clienttasks.getCurrentEntitlementCerts().isEmpty(), "Due to a failed attempt to set a service level during register with autosubscribe, we should not be consuming any entitlements.");	
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19991", "RHEL7-51023"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: register with autosubscribe while specifying an valid service level; assert the entitlements granted match the requested service level",
			groups={"Tier1Tests","blockedByBug-859652","blockedByBug-919700"},
			// dataProvider="getAllAvailableServiceLevelData",	// 06/05/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithAvailableServiceLevel(Object bugzilla, String serviceLevel) throws JSONException, Exception {
		// Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe

		// register with autosubscribe specifying a valid service level
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, serviceLevel, null, (String)null, null, null, null, true, null, null, null, null, null));
		
		// get the current consumer object and assert that the serviceLevel persisted
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertEquals(jsonConsumer.get("serviceLevel"), serviceLevel, "The call to register with autosubscribe and a servicelevel persisted the servicelevel setting on the current consumer object.");
		
		// assert that each of the autosubscribed entitlements come from a pool that supports the specified service level
		clienttasks.listConsumedProductSubscriptions();
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
			
			// tolerate entitlements granted from pools with null/"" support_level regardless of the specified service level
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">="/*TODO ">" is technically correct*/, "2.0.2-1")) {	// commit 9cefb6e23baefcc4ee2e14423f205edd37eecf22	// Bug 1223560 - Service levels on an activation key prevent custom products from attaching at registration if auto-attach enabled
				if (entitlementCert.orderNamespace.supportLevel==null || entitlementCert.orderNamespace.supportLevel.isEmpty()) {
					log.warning("Regardless of the consumer's service-level preference '"+jsonConsumer.get("serviceLevel")+"' or the requested service-level '"+serviceLevel+"', this EntitlementCert provides a support_level of '"+entitlementCert.orderNamespace.supportLevel+"'. (New behavior modification from Bug 1223560)");
					continue;
				}
			}
			
			if (sm_exemptServiceLevelsInUpperCase.contains(entitlementCert.orderNamespace.supportLevel.toUpperCase())) {
				log.warning("After autosubscribed registration with service level '"+serviceLevel+"', this autosubscribed entitlement provides an exempt service level '"+entitlementCert.orderNamespace.supportLevel+"' from entitled orderNamespace: "+entitlementCert.orderNamespace);
			} else {
				//CASE SENSITIVE ASSERTION Assert.assertEquals(entitlementCert.orderNamespace.supportLevel, serviceLevel,"This autosubscribed entitlement provides the requested service level '"+serviceLevel+"' from entitled orderNamespace: "+entitlementCert.orderNamespace);
				Assert.assertTrue(entitlementCert.orderNamespace.supportLevel.equalsIgnoreCase(serviceLevel),"Ignoring case, this autosubscribed entitlement provides the requested service level '"+serviceLevel+"' from entitled orderNamespace: "+entitlementCert.orderNamespace);
			}
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19995", "RHEL7-51028"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: register with autosubscribe while specifying an valid random case SeRviCEleVel; assert the installed product status is independent of the specified service level case.",
			groups={"Tier1Tests","blockedByBug-859652","blockedByBug-859652","blockedByBug-919700"},
			// dataProvider="getAllAvailableServiceLevelData",	// 06/05/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithServiceLevelIsCaseInsensitive(Object bugzilla, String serviceLevel) {
		
		// TEMPORARY WORKAROUND FOR BUG
		if (sm_serverType.equals(CandlepinType.hosted)) {
		String bugId = "818319"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			throw new SkipException("Hosted candlepin server '"+sm_serverHostname+"' does not yet support this test execution.");
		}
		}
		// END OF WORKAROUND
		
		// register with autosubscribe specifying a valid service level and get the installed product status
		List<InstalledProduct> installedProductsAfterAutosubscribedRegisterWithServiceLevel= InstalledProduct.parse(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, serviceLevel, null, (String)null, null, null, null, true, null, null, null, null, null).getStdout());
		
		// register with autosubscribe specifying a mixed case service level and get the installed product status
		String mixedCaseServiceLevel = randomizeCaseOfCharactersInString(serviceLevel);
		List<InstalledProduct> installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel= InstalledProduct.parse(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, mixedCaseServiceLevel, null, (String)null, null, null, null, true, null, null, null, null, null).getStdout());

		// assert that the two lists are identical (independent of the serviceLevel case specified during registration)
		Assert.assertEquals(installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel.size(), clienttasks.getCurrentProductIds().size(), "The registration output displayed the same number of installed product status's as the number of installed product certs.");
		Assert.assertTrue(installedProductsAfterAutosubscribedRegisterWithServiceLevel.containsAll(installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel) && installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel.containsAll(installedProductsAfterAutosubscribedRegisterWithServiceLevel), "Autosubscribed registration with serviceLevel '"+mixedCaseServiceLevel+"' yielded the same installed product status as autosubscribed registration with serviceLevel '"+serviceLevel+"'.");
		
		// assert that each of the consumed ProductSubscriptions match the specified service level
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		if (consumedProductSubscriptions.isEmpty()) log.warning("No entitlements were granted after registering with autosubscribe and service level '"+mixedCaseServiceLevel+"'."); 
		for (ProductSubscription productSubscription : consumedProductSubscriptions) {
			
			// tolerate ProductSubscriptions with a null/"" serviceLevel. (result of candlepin Bug 1223560)
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">="/*TODO ">" is technically correct*/, "2.0.2-1")) {	// commit 9cefb6e23baefcc4ee2e14423f205edd37eecf22	// Bug 1223560 - Service levels on an activation key prevent custom products from attaching at registration if auto-attach enabled
				if (productSubscription.serviceLevel==null || productSubscription.serviceLevel.isEmpty()) {
					log.warning("After autosubscribed registration with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides no service level '"+productSubscription.serviceLevel+"'.  (New behavior modification from Bug 1223560)");
					continue;
				}
			}
			
			// tolerate exemptServiceLevels
			if (sm_exemptServiceLevelsInUpperCase.contains(productSubscription.serviceLevel.toUpperCase())) {
				log.warning("After autosubscribed registration with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides an exempt service level '"+productSubscription.serviceLevel+"'.");
				continue;
			}
			
			Assert.assertTrue(productSubscription.serviceLevel.equalsIgnoreCase(mixedCaseServiceLevel),
					"After autosubscribed registration with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides a service level '"+productSubscription.serviceLevel+"' that is a case insensitive match to '"+mixedCaseServiceLevel+"'.");
		}
	}






	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21862", "RHEL7-51730"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: subscribe with auto while specifying an unavailable service level",
			groups={"Tier3Tests","blockedByBug-795798","blockedByBug-864508"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAutoSubscribeWithUnavailableServiceLevel() {
		
		// register with force
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// subscribe with auto specifying an unavailable service level
		SSHCommandResult result = clienttasks.subscribe_(true,"FOO",(String)null,null,null,null,null,null,null, null, null, null, null);
		Integer expectedExitCode = new Integer(255);
		String expectedStderr = "";
		String expectedStdout = "Cannot set a service level for a consumer that is not available to its organization.";
		expectedStdout = String.format("Service level %s is not available to consumers of organization %s.","FOO",sm_clientOrg);	// valid before bug fix 864508
		expectedStdout = String.format("Service level '%s' is not available to consumers of organization %s.","FOO",sm_clientOrg);// valid before bug fix 864508
		if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedStdout = String.format("Service level '%s' is not available to units of organization %s.","FOO",sm_clientOrg);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			expectedStdout = String.format("Service level \"%s\" is not available to units of organization %s.","FOO",sm_clientOrg);
		}
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {String swap=expectedStderr; expectedStderr=expectedStdout; expectedStdout=swap;}	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) expectedStderr = "HTTP error code 400: "+expectedStderr;	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "Exit code from an attempt to subscribe with auto and an unavailable service level.");
		Assert.assertEquals(result.getStdout().trim(), expectedStdout, "Stdout from an attempt to subscribe with auto and an unavailable service level.");
		Assert.assertEquals(result.getStderr().trim(), expectedStderr, "Stderr from an attempt to subscribe with auto and an unavailable service level.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19990", "RHEL7-51022"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: subscribe with auto while specifying an valid service level; assert the entitlements granted match the requested service level",
			groups={"Tier1Tests","blockedByBug-859652","blockedByBug-977321"},
			// dataProvider="getAllAvailableServiceLevelData",	// 06/05/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfAllAvailableServiceLevelData",
			enabled=true)
	@ImplementsNitrateTest(caseId=157229)	// 147971
	public void testAutoSubscribeWithServiceLevel(Object bugzilla, String serviceLevel) throws JSONException, Exception {
		// Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe
		
		// ensure system is registered
		if (clienttasks.getCurrentConsumerId()==null) clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "AutoSubscribeWithServiceLevelConsumer", null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		// are any products installed?
		boolean noInstalledProducts = clienttasks.getCurrentlyInstalledProducts().isEmpty();
		
		// remember this initial service level preference for this consumer
		String initialConsumerServiceLevel = clienttasks.getCurrentServiceLevel();
		
		// start fresh by returning all entitlements
		//clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.unsubscribeFromTheCurrentlyConsumedSerialsCollectively();	// may help avoid: Runtime Error No row with the given identifier exists: [org.candlepin.model.PoolAttribute#8a99f98146b4fa9d0146b7e4d5d34375] at org.hibernate.UnresolvableObjectException.throwIfNull:64
		
		// autosubscribe with a valid service level
		SSHCommandResult subscribeResult = clienttasks.subscribe(true,serviceLevel,(String)null,(String)null,(String)null,null,null,null,null, null, null, null, null);
		
		// get the current consumer object and assert that the serviceLevel persisted
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+clienttasks.getCurrentConsumerId()));
		if (serviceLevel==null) {
			Assert.assertEquals(jsonConsumer.get("serviceLevel"), initialConsumerServiceLevel, "The consumer's serviceLevel preference should remain unchanged when calling subscribe with auto and a servicelevel of null.");
		} else if (noInstalledProducts) {
			Assert.assertEquals(jsonConsumer.get("serviceLevel"), initialConsumerServiceLevel, "The consumer's serviceLevel preference should remain unchanged when calling subscribe with auto when no products are installed because the autosubscribe process should abort thereby not attenpting a service level change.");
		} else {
			Assert.assertEquals(jsonConsumer.get("serviceLevel"), serviceLevel, "The call to subscribe with auto and a servicelevel of '"+serviceLevel+"' persisted the servicelevel setting on the current consumer object.");			
		}
		
		// assert that each of the autosubscribed entitlements come from a pool that supports the specified service level
		List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
		if (subscribeResult.getExitCode().intValue()==1) Assert.assertEquals(entitlementCerts.size(), 0, "When subscribe --auto returns an exitCode of 1, then no entitlements should have been granted.");
		for (EntitlementCert entitlementCert : entitlementCerts) {
			
			// tolerate entitlements granted from pools with null/no support_level regardless of the specified service level
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">="/*TODO ">" is technically correct*/, "2.0.2-1")) {	// commit 9cefb6e23baefcc4ee2e14423f205edd37eecf22	// Bug 1223560 - Service levels on an activation key prevent custom products from attaching at registration if auto-attach enabled
				if (entitlementCert.orderNamespace.supportLevel==null || entitlementCert.orderNamespace.supportLevel.isEmpty()) {
					log.warning("Regardless of the consumer's service-level preference '"+initialConsumerServiceLevel+"' or the requested service-level '"+serviceLevel+"', this EntitlementCert provides a support_level of '"+entitlementCert.orderNamespace.supportLevel+"'. (New behavior modification from Bug 1223560)");
					continue;
				}
			}
			
			// tolerate exemptServiceLevels
			if (entitlementCert.orderNamespace.supportLevel!=null && sm_exemptServiceLevelsInUpperCase.contains(entitlementCert.orderNamespace.supportLevel.toUpperCase())) {
				log.warning("After autosubscribing, this EntitlementCert provides an exempt service level '"+entitlementCert.orderNamespace.supportLevel+"'.");
				continue;
			}
			
			if ("".equals(serviceLevel) || (serviceLevel==null && initialConsumerServiceLevel.equals(""))) {
				log.info("When specifying a servicelevel of \"\" during an autosubscribe (or specifying a servicelevel of null and the current consumer's has no servicelevel preference), then the servicelevel of the granted entitlement certs can be anything.  This one is '"+entitlementCert.orderNamespace.supportLevel+"'.");
			} else if (serviceLevel==null && !initialConsumerServiceLevel.equals("")){
				Assert.assertTrue(initialConsumerServiceLevel.equalsIgnoreCase(entitlementCert.orderNamespace.supportLevel), "When specifying a servicelevel of null during an autosubscribe and the current consumer has a servicelevel preference set, then the servicelevel from the orderNamespace of this granted entitlement cert ("+entitlementCert.orderNamespace.supportLevel+") must match the current consumer's service level preference ("+initialConsumerServiceLevel+").");
			} else {
				Assert.assertTrue(serviceLevel.equalsIgnoreCase(entitlementCert.orderNamespace.supportLevel), "Ignoring case, this autosubscribed entitlement was filled from a subscription order that provides the requested service level '"+serviceLevel+"': "+entitlementCert.orderNamespace);
			}
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21861", "RHEL7-51724"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: after autosubscribing with a service level, assert that another autosubscribe (without specifying service level) uses the service level persisted from the first sutosubscribe",
			groups={"Tier3Tests","blockedByBug-859652","blockedByBug-977321"},
			dependsOnMethods={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAutoSubscribeAgainAssertingServiceLevelIsPersisted() throws JSONException, Exception {
		// Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe

		// get all the valid service levels available to this org	
		List<String> serviceLevelsExpected = CandlepinTasks.getServiceLevelsForOrgKey(/*sm_serverAdminUsername*/sm_clientUsername, /*sm_serverAdminPassword*/sm_clientPassword, sm_serverUrl, sm_clientOrg);
		String serviceLevel = serviceLevelsExpected.get(randomGenerator.nextInt(serviceLevelsExpected.size()));
		
		//clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, serviceLevel, null, (String)null, null, false, null, null, null);
		testAutoSubscribeWithServiceLevel(null,serviceLevel);

		// return all entitlements
		clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);
		
		// autosubscribe again without specifying a service level
		clienttasks.subscribe(true,null,(String)null,(String)null,(String)null,null,null,null,null, null, null, null, null);
		
		// get the current consumer object and assert that the serviceLevel persisted even though the subscribe did NOT specify a service level
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), serviceLevel, "The call to subscribe with auto (without specifying a servicelevel) did not alter current servicelevel.");

		// assert that each of the autosubscribed entitlements come from a pool that supports the original service level
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
			
			// tolerate entitlements granted from pools with null/no support_level regardless of the specified service level
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">="/*TODO ">" is technically correct*/, "2.0.2-1")) {	// commit 9cefb6e23baefcc4ee2e14423f205edd37eecf22	// Bug 1223560 - Service levels on an activation key prevent custom products from attaching at registration if auto-attach enabled
				if (entitlementCert.orderNamespace.supportLevel==null || entitlementCert.orderNamespace.supportLevel.isEmpty()) {
					log.warning("Regardless of the consumer's original service level '"+serviceLevel+"', this EntitlementCert provides a support_level of '"+entitlementCert.orderNamespace.supportLevel+"'. (New behavior modification from Bug 1223560)");
					continue;
				}
			}
			
			// tolerate exemptServiceLevels
			if (sm_exemptServiceLevelsInUpperCase.contains(entitlementCert.orderNamespace.supportLevel.toUpperCase())) {
				log.warning("Regardless of the consumer's original service level '"+serviceLevel+"', this EntitlementCert provides an exempt service level '"+entitlementCert.orderNamespace.supportLevel+"'.");
				continue;
			}

			//CASE SENSITIVE ASSERTION Assert.assertEquals(entitlementCert.orderNamespace.supportLevel, serviceLevel,"This autosubscribed EntitlementCert was filled from a subscription order that provides the original service level '"+serviceLevel+"': "+entitlementCert.orderNamespace);
			Assert.assertTrue(entitlementCert.orderNamespace.supportLevel.equalsIgnoreCase(serviceLevel),"Ignoring case, this autosubscribed EntitlementCert was filled from a subscription order that provides the original service level '"+serviceLevel+"': "+entitlementCert.orderNamespace);
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19989", "RHEL7-33078"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: subscribe with auto without specifying any service level; assert the service level used matches whatever the consumer's current preference level is set",
			groups={"Tier1Tests","blockedByBug-859652","blockedByBug-977321"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAutoSubscribeWithNullServiceLevel() throws JSONException, Exception {
		testAutoSubscribeWithServiceLevel(null,null);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19988", "RHEL7-51021"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: subscribe with auto specifying a service level of \"\"; assert the service level is unset and the autosubscribe proceeds without any service level preference",
			groups={"Tier1Tests","blockedByBug-859652","blockedByBug-977321","blockedByBug-1001169"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAutoSubscribeWithBlankServiceLevel() throws JSONException, Exception {
		testAutoSubscribeWithServiceLevel(null,"");
		
		// adding the following instructions specifically to force the testing of bug 1001169
		List<String> availableServiceLevels = clienttasks.getCurrentlyAvailableServiceLevels();
		if (availableServiceLevels.isEmpty()) throw new SkipException("Skipping the remainder of this test when there are no available service levels.");
		String randomAvailableServiceLevel = availableServiceLevels.get(randomGenerator.nextInt(availableServiceLevels.size()));
		clienttasks.service_level(null, null, randomAvailableServiceLevel, null, null, null, null, null, null, null, null, null, null);
		testAutoSubscribeWithServiceLevel(null,"");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19994", "RHEL7-51026"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: autosubscribe while specifying an valid service level; assert the installed product status is independent of the specified SerViceLeVEL case.",
			groups={"Tier1Tests","blockedByBug-818319","blockedByBug-859652"},
			// dataProvider="getAllAvailableServiceLevelData",	// 06/05/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfAllAvailableServiceLevelData",
			enabled=true)
	@ImplementsNitrateTest(caseId=157227) // 157226 //157225
	public void testAutoSubscribeWithServiceLevelIsCaseInsensitive(Object bugzilla, String serviceLevel) throws JSONException, Exception {
		
		// TEMPORARY WORKAROUND FOR BUG
		if (sm_serverType.equals(CandlepinType.hosted)) {
		String bugId = "818319"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			throw new SkipException("This test is blocked by Bugzilla https://bugzilla.redhat.com/show_bug.cgi?id="+bugId);
		}
		}
		// END OF WORKAROUND
		
		// system was already registered by dataProvider="getSubscribeWithAutoAndServiceLevelData"
		if (clienttasks.getCurrentConsumerId()==null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		}
		
		// start fresh by returning all entitlements
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
	
		// autosubscribe specifying a valid service level and get the installed product status
		List<InstalledProduct> installedProductsAfterAutosubscribingWithServiceLevel= InstalledProduct.parse(clienttasks.subscribe(true, serviceLevel, (String)null, (String)null, (String)null, null, null, null, null, null, null, null, null).getStdout());
		
		// unsubscribe from all entitlements
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		// autosubscribe specifying a mixed case service level and get the installed product status
		String mixedCaseServiceLevel = randomizeCaseOfCharactersInString(serviceLevel);
		List<InstalledProduct> installedProductsAfterAutosubscribingWithMixedCaseServiceLevel= InstalledProduct.parse(clienttasks.subscribe(true, mixedCaseServiceLevel, (String)null, (String)null, (String)null, null, null, null, null, null, null, null, null).getStdout());

		// assert that the two lists are identical (independent of the serviceLevel case specified during autosubscribe)
		Assert.assertEquals(installedProductsAfterAutosubscribingWithMixedCaseServiceLevel.size(), clienttasks.getCurrentProductIds().size(), "The subscribe output displayed the same number of installed product status's as the current number of installed product certs.");
		Assert.assertTrue(installedProductsAfterAutosubscribingWithServiceLevel.containsAll(installedProductsAfterAutosubscribingWithMixedCaseServiceLevel) && installedProductsAfterAutosubscribingWithMixedCaseServiceLevel.containsAll(installedProductsAfterAutosubscribingWithServiceLevel), "Autosubscribe with serviceLevel '"+mixedCaseServiceLevel+"' yielded the same installed product status as autosubscribe with serviceLevel '"+serviceLevel+"'.");
		
		// get the current exempt service levels
		List<String> exemptServiceLevels = CandlepinTasks.getServiceLevelsForOrgKey(sm_clientUsername, sm_clientPassword, sm_serverUrl, clienttasks.getCurrentlyRegisteredOwnerKey(), true);
		List<String> exemptServiceLevelsInUpperCase = new ArrayList<String>();
		for (String exemptServiceLevel : exemptServiceLevels) exemptServiceLevelsInUpperCase.add(exemptServiceLevel.toUpperCase());
		
		// assert that each of the consumed ProductSubscriptions match the specified service level
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		if (consumedProductSubscriptions.isEmpty()) log.warning("No entitlements were granted after autosubscribing with service level '"+mixedCaseServiceLevel+"'.");
		for (ProductSubscription productSubscription : consumedProductSubscriptions) {
			
			// tolerate ProductSubscriptions with a null/"" serviceLevel. (result of candlepin Bug 1223560)
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">="/*TODO ">" is technically correct*/, "2.0.2-1")) {	// commit 9cefb6e23baefcc4ee2e14423f205edd37eecf22	// Bug 1223560 - Service levels on an activation key prevent custom products from attaching at registration if auto-attach enabled
				if (productSubscription.serviceLevel==null || productSubscription.serviceLevel.isEmpty()) {
					log.warning("After autosubscribe with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides no service level '"+productSubscription.serviceLevel+"'.  (New behavior modification from Bug 1223560)");
					continue;
				}
			}
			
			// tolerate ProductSubscriptions with exemptServiceLevels
			if (/*sm_*/exemptServiceLevelsInUpperCase.contains(productSubscription.serviceLevel.toUpperCase())) {
				log.warning("After autosubscribe with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides an exempt service level '"+productSubscription.serviceLevel+"'.");
				continue;
			}
			
			Assert.assertTrue(productSubscription.serviceLevel.equalsIgnoreCase(mixedCaseServiceLevel),
						"After autosubscribe with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides a service level '"+productSubscription.serviceLevel+"' that is a case insensitive match to '"+mixedCaseServiceLevel+"'.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21703", "RHEL7-51027"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="installed products provided by available pools with an exempt service level should be auto-subscribed regardless of what service level is specified (or is not specified)",
			groups={"Tier1Tests","blockedByBug-818319","blockedByBug-859652","blockedByBug-919700"},
			dataProvider="getExemptInstalledProductAndServiceLevelData",
			enabled=true)
	@ImplementsNitrateTest(caseId=157229)
	public void testInstalledProductsProvidedByAvailablePoolsWithExemptServiceLevelAreAutoSubscribedRegardlessOfServiceLevel(Object bugzilla, String installedProductId, String serviceLevel) {
		
		// randomize the case of the service level
		String seRvICElevEl = randomizeCaseOfCharactersInString(serviceLevel);
		log.info("This test will be conducted with a randomized serviceLevel value: "+seRvICElevEl);

		// TEMPORARY WORKAROUND FOR BUG
		if (sm_serverType.equals(CandlepinType.hosted)) {
		String bugId = "818319"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			seRvICElevEl = serviceLevel;
			log.warning("This test will NOT be conducted with a randomized serviceLevel value.  Testing with serviceLevel: "+seRvICElevEl);
		}
		}
		// END OF WORKAROUND
		
		// register with autosubscribe and a randomize case serviceLevel
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, seRvICElevEl, null, (List<String>)null, null, null, null, true, false, null, null, null, null);
		
		// assert that the installed ProductId is "Subscribed"
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", installedProductId, clienttasks.getCurrentlyInstalledProducts());
		Assert.assertEquals(installedProduct.status, "Subscribed", "After registering with autosubscribe and serviceLevel '"+seRvICElevEl+"' the following installed exempt product should be subscribed: "+installedProduct);
		
		// EXTRA CREDIT: assert that the consumed ProductSubscription that provides the installed exempt product is among the known exempt service levels.
		// WARNING: THIS MAY NOT BE AN APPROPRIATE ASSERTION WHEN THE EXEMPT PRODUCT HAPPENS TO ALSO BE PROVIDED BY A SUBSCRIPTION THAT COINCIDENTY PROVIDES ANOTHER INSTALLED PRODUCT
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
			for (ProductNamespace productNamespace : entitlementCert.productNamespaces) {
				if (productNamespace.id.equals(installedProductId)) {
					BigInteger serialNumber = clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCert.file);
					ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("serialNumber", serialNumber, productSubscriptions);
					Assert.assertTrue(sm_exemptServiceLevelsInUpperCase.contains(productSubscription.serviceLevel.toUpperCase()),"Installed exempt product '"+installedProduct+"' is entitled by consumed productSubscription '"+productSubscription+"' that provides one of the exempt service levels '"+sm_exemptServiceLevelsInUpperCase+"'.");
				}
			}
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21869", "RHEL7-51714"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Using curl, set the default service level for an org and then register using org credentials to verify consumer's service level",
			groups={"Tier3Tests","SetDefaultServiceLevelForOrgAndRegister_Test"},
			// dataProvider="getAllAvailableServiceLevelData",	// 06/05/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSetDefaultServiceLevelForOrgAndRegister(Object bugzilla, String defaultServiceLevel) throws JSONException, Exception {
		
		// update the defaultServiceLevel on the Org
		if (orgForSetDefaultServiceLevelForOrgAndRegister_Test==null) orgForSetDefaultServiceLevelForOrgAndRegister_Test = clienttasks.getCurrentlyRegisteredOwnerKey();
		JSONObject jsonOrg = CandlepinTasks.setAttributeForOrg(sm_clientUsername, sm_clientPassword, sm_serverUrl, orgForSetDefaultServiceLevelForOrgAndRegister_Test, "defaultServiceLevel", defaultServiceLevel);
		Assert.assertEquals(jsonOrg.get("defaultServiceLevel"), defaultServiceLevel, "The defaultServiceLevel update to org '"+orgForSetDefaultServiceLevelForOrgAndRegister_Test+"' appears successful on the candlepin server.");
		
		// register and assert the consumer's service level is set to the new org default
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg/*will either be null or equal to orgForSetDefaultServiceLevelForOrgAndRegister_Test*/, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), defaultServiceLevel, "Immediately upon registering, the consumer's service level preference was set to the org's default.");
	}
	protected String orgForSetDefaultServiceLevelForOrgAndRegister_Test = null;
	@AfterGroups(value={"SetDefaultServiceLevelForOrgAndRegister_Test"},groups={"setup"})
	public void afterSetDefaultServiceLevelForOrgAndRegister_Test() throws Exception {
		// update the defaultServiceLevel on the Org (setting to "" will nullify the attribute on the org; setting to JSONObject.NULL does not work)
		if (orgForSetDefaultServiceLevelForOrgAndRegister_Test!=null) {
			JSONObject jsonOrg = CandlepinTasks.setAttributeForOrg(sm_clientUsername, sm_clientPassword, sm_serverUrl, orgForSetDefaultServiceLevelForOrgAndRegister_Test, "defaultServiceLevel", "");
			jsonOrg.get("defaultServiceLevel"); // should be null
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21880", "RHEL7-51732"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Using curl, unset the default service level for an org and then register using org credentials to verify consumer's service level is not set",
			groups={"Tier3Tests"},
			dependsOnMethods={"testSetDefaultServiceLevelForOrgAndRegister"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsetDefaultServiceLevelForOrgAndRegister() throws JSONException, Exception {
		
		// update the defaultServiceLevel on the Org (setting to "" will nullify the attribute on the org; setting to JSONObject.NULL does not work)
		JSONObject jsonOrg = CandlepinTasks.setAttributeForOrg(sm_clientUsername, sm_clientPassword, sm_serverUrl, orgForSetDefaultServiceLevelForOrgAndRegister_Test, "defaultServiceLevel", "");
		Assert.assertEquals(jsonOrg.get("defaultServiceLevel"), JSONObject.NULL, "The defaultServiceLevel update to the org appears successful on the candlepin server.");
		
		// register and assert the consumer's service level is not set
		clienttasks.register(sm_clientUsername, sm_clientPassword, orgForSetDefaultServiceLevelForOrgAndRegister_Test, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), "", "Immediately upon registering, the consumer's service level preference was set to the org's default (which was unset).");
	}



	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21873", "RHEL7-51728"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --set (with unavailable serviceLevel)",
			groups={"Tier3Tests","blockedByBug-864508"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelSetWithUnavailableServiceLevel() throws JSONException, Exception {
			
		// test while registered
		if (clienttasks.getCurrentConsumerId()==null) clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg);
		String org = clienttasks.getCurrentlyRegisteredOwnerKey();	// current org (in case sm_clientOrg is null)
		
		String unavailableSericeLevel = "FOO";
		SSHCommandResult result = clienttasks.service_level_(null, null, unavailableSericeLevel, null, null, null, null, null, null, null, null, null, null);
		String expectedStderr = String.format("Service level %s is not available to consumers of organization %s.",unavailableSericeLevel,org); 	// valid before bug fix 864508
		expectedStderr = String.format("Service level '%s' is not available to consumers of organization %s.",unavailableSericeLevel,org);
		if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedStderr = String.format("Service level '%s' is not available to units of organization %s.",unavailableSericeLevel,org);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			expectedStderr = String.format("Service level \"%s\" is not available to units of organization %s.",unavailableSericeLevel,org);
		}
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "ExitCode from service-level --set with unavailable serviceLevel");
		Assert.assertEquals(result.getStderr().trim(), expectedStderr, "Stderr from service-level --set with unavailable serviceLevel");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --set with unavailable serviceLevel");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21867", "RHEL7-51716"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --set (with exempt serviceLevel should be treated as unavailable)",
			groups={"Tier3Tests","blockedByBug-864508"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelSetWithExemptServiceLevel() {
			
		// test while registered
		if (clienttasks.getCurrentConsumerId()==null) clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,null, null, null, null, null, null);
		
		String exemptServiceLevel = sm_exemptServiceLevelsInUpperCase.get(SubscriptionManagerCLITestScript.randomGenerator.nextInt(sm_exemptServiceLevelsInUpperCase.size()));
		SSHCommandResult result = clienttasks.service_level_(null, null, exemptServiceLevel, null, null, null, null, null, null, null, null, null, null);
		log.info("An exempt service level should be treated as an unavailable service level when attempting to set.");
		String expectedStderr = String.format("Service level %s is not available to consumers of organization admin.",exemptServiceLevel);	// valid before bug fix 864508
		expectedStderr = String.format("Service level '%s' is not available to consumers of organization admin.",exemptServiceLevel);
		if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedStderr = String.format("Service level '%s' is not available to units of organization admin.",exemptServiceLevel);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			expectedStderr = String.format("Service level \"%s\" is not available to units of organization admin.",exemptServiceLevel);
		}
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "ExitCode from service-level --set with exempt serviceLevel");
		Assert.assertEquals(result.getStderr().trim(), expectedStderr, "Stderr from service-level --set with exempt serviceLevel");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --set with exempt serviceLevel");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21875", "RHEL7-51718"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --set (with available serviceLevel)",
			groups={"Tier3Tests"},
			// dataProvider="getAllAvailableServiceLevelData",	// 06/05/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelSetWithAvailableServiceLevel(Object bugzilla, String serviceLevel) {
		
		// no need to register ("getAllAvailableServiceLevelData" will re-register with force)
		
		clienttasks.service_level(null, null, serviceLevel, null, null, null, null, null, null, null, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), serviceLevel, "The --set serviceLevel matches the current --show serviceLevel.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21870", "RHEL7-51715"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --set should accept \"\" to effectively unset",
			groups={"Tier3Tests","blockedByBug-835050"},
			// dataProvider="getAllAvailableServiceLevelData",	// 06/05/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsetServiceLevel(Object bugzilla, String serviceLevel) {
		
		testServiceLevelSetWithAvailableServiceLevel(bugzilla,serviceLevel);
		clienttasks.service_level(null, null, "", null, null, null, null, null, null, null, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), "", "The serviceLevel can effectively be unset by setting a value of \"\".");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21866", "RHEL7-51731"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --set (with case insensitivity) is preserved throughtout an identity regeneration",
			groups={"Tier3Tests"},
			// dataProvider="getAllAvailableServiceLevelData",	// 06/05/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelSetWithAvailableServiceLevelIsPreservedThroughIdentityRegeneration(Object bugzilla, String serviceLevel) throws JSONException, Exception {
		
		// no need to register ("getAllAvailableServiceLevelData" will re-register with force)
		
		serviceLevel = randomizeCaseOfCharactersInString(serviceLevel);
		clienttasks.service_level(null, null, serviceLevel, null, null, null, null, null, null, null, null, null, null);
		clienttasks.identity(null,null,true,null,null,null,null, null);
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), serviceLevel, "The --set serviceLevel matches the current --show serviceLevel even after an identity regeneration.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21876", "RHEL7-51720"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --unset after setting an available service level",
			groups={"Tier3Tests","blockedByBug-829803","blockedByBug-829812"},
			// dataProvider="getAllAvailableServiceLevelData",	// 06/05/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelUnsetAfterSet(Object bugzilla, String serviceLevel) {
		
		testServiceLevelSetWithAvailableServiceLevel(bugzilla,serviceLevel);
		clienttasks.service_level(null, null, null, true, null, null, null, null, null, null, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), "", "The serviceLevel is unset after calling with the --unset option.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19992", "RHEL7-51024"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: service-level --list should be unique (regardless of case)",
			groups={"Tier1Tests","blockedByBug-845043"},
			dataProvider="getRegisterCredentialsExcludingNullOrgData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelListIsUnique(String username, String password, String org) {
		
		clienttasks.register(username, password, org,null,null,null,null,null,null,null,(String)null,null,null,null,true,null,null,null, null, null);
		
		List<String> availServiceLevels = clienttasks.getCurrentlyAvailableServiceLevels();
		Set<String> uniqueServiceLevels = new HashSet<String>();
		for (String availServiceLevel : availServiceLevels) uniqueServiceLevels.add(availServiceLevel.toUpperCase());
		
		Assert.assertTrue(uniqueServiceLevels.size()==availServiceLevels.size(), "The available service levels are unique. There are no duplicates (regardless of case).");
	}
	
	
	
	protected String server_hostname = null;
	protected String server_port = null;
	protected String server_prefix = null;
	protected String clientOrg = null;
	@BeforeGroups(value={"ServiceLevelListWithServerurl_Test"}, groups={"setup"})
	public void beforeServiceLevelListWithServerurl_Test() {
		if (clienttasks==null) return;
		server_hostname	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		server_port		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
		server_prefix	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
		clientOrg 		= clienttasks.getOrgs(sm_clientUsername,sm_clientPassword).get(0).orgKey;	// use the first org
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21879", "RHEL7-51719"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level --list with --serverurl",
			dataProvider="getServerurlData",
			groups={"Tier3Tests","ServiceLevelListWithServerurl_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelListWithServerurl(Object bugzilla, String serverurl, String expectedHostname, String expectedPort, String expectedPrefix, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrMatch) {
		// get original server at the beginning of this test
		String hostnameBeforeTest	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname");
		String portBeforeTest		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port");
		String prefixBeforeTest		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix");
		
		// service-level --list with a serverurl
		SSHCommandResult sshCommandResult = clienttasks.service_level_(null,true,null,null,sm_clientUsername,sm_clientPassword,clientOrg,serverurl, null, null, null, null, null);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null)	Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode,"ExitCode after register with --serverurl="+serverurl+" and other options:");
		if (expectedStdoutRegex!=null)	Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), expectedStdoutRegex,"Stdout after register with --serverurl="+serverurl+" and other options:");
		if (expectedStderrMatch!=null)	Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), expectedStderrMatch,"Stderr after register with --serverurl="+serverurl+" and other options:");
		Assert.assertContainsNoMatch(sshCommandResult.getStderr().trim(), "Traceback.*","Stderr after register with --serverurl="+serverurl+" and other options should not contain a Traceback.");
		
		// negative testcase assertions........
		//if (expectedExitCode.equals(new Integer(255))) {
		if (Integer.valueOf(expectedExitCode)>1) {
			// assert that the current config remains unchanged when the expectedExitCode indicates an error
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"), hostnameBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [server] hostname should remain unchanged when attempting to register with an invalid serverurl.");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"),	portBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [server] port should remain unchanged when attempting to register with an invalid serverurl.");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"), prefixBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [server] prefix should remain unchanged when attempting to register with an invalid serverurl.");
						
			return;	// nothing more to do after these negative testcase assertions
		}
		
		// positive testcase assertions........
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"), expectedHostname, "The "+clienttasks.rhsmConfFile+" configuration for [server] hostname has been updated from the specified --serverurl "+serverurl);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"), expectedPort, "The "+clienttasks.rhsmConfFile+" configuration for [server] port has been updated from the specified --serverurl "+serverurl);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"), expectedPrefix, "The "+clienttasks.rhsmConfFile+" configuration for [server] prefix has been updated from the specified --serverurl "+serverurl);
	}
	@AfterGroups(value={"ServiceLevelListWithServerurl_Test"},groups={"setup"})
	public void afterRegisterWithServerurl_Test() {
		if (server_hostname!=null)	clienttasks.config(null,null,true,new String[]{"server","hostname",server_hostname});
		if (server_port!=null)		clienttasks.config(null,null,true,new String[]{"server","port",server_port});
		if (server_prefix!=null)	clienttasks.config(null,null,true,new String[]{"server","prefix",server_prefix});
	}
	

	protected String rhsm_ca_cert_dir = null;
	@BeforeGroups(value={"ServiceLevelListWithInsecure_Test"}, groups={"setup"})
	public void beforeServiceLevelListWithInsecure_Test() {
		if (clienttasks==null) return;
		rhsm_ca_cert_dir	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "ca_cert_dir");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21864", "RHEL7-51723"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: service-level list with --insecure",
			groups={"Tier3Tests","ServiceLevelListWithInsecure_Test","blockedByBug-844411","blockedByBug-993202","blockedByBug-1256960"/*is a duplicate of*/,"blockedByBug-1254349"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testServiceLevelListWithInsecure() {
		SSHCommandResult sshCommandResult;
		
		// calling service level list without insecure should pass
		sshCommandResult = clienttasks.service_level(null,true,null,null,sm_clientUsername,sm_clientPassword, sm_clientOrg, null, false, null, null, null, null);
		
		// change the rhsm.ca_cert_dir configuration to simulate a missing candlepin ca cert
		client.runCommandAndWait("mkdir -p /tmp/emptyDir");
		sshCommandResult = clienttasks.config(null, null, true, new String[]{"rhsm","ca_cert_dir","/tmp/emptyDir/"});
		
		// calling service level list without insecure should now fail (throwing stderr "certificate verify failed")
		sshCommandResult = clienttasks.service_level_(null,true,null,null,sm_clientUsername,sm_clientPassword, sm_clientOrg, null, false, null, null, null, null);
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode, "Exitcode from the service-level list command when configuration rhsm.ca_cert_dir has been falsified.");
		if (clienttasks.isPackageVersion("python-rhsm",">=","1.18.5-1") && Integer.valueOf(clienttasks.redhatReleaseX)>=7) {	// post python-rhsm commit 214103dcffce29e31858ffee414d79c1b8063970   Reduce usage of m2crypto (#184) (RHEL7+)
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "Unable to verify server's identity: [SSL: CERTIFICATE_VERIFY_FAILED] certificate verify failed (_ssl.c:579)", "Stderr from the service-level list command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from the service-level list command when configuration rhsm.ca_cert_dir has been falsified.");
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "Unable to verify server's identity: certificate verify failed", "Stderr from the service-level list command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from the service-level list command when configuration rhsm.ca_cert_dir has been falsified.");
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.9-1")) {	// post commit 3366b1c734fd27faf48313adf60cf051836af115
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr from the service-level list command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "Unable to verify server's identity: certificate verify failed", "Stdout from the service-level list command when configuration rhsm.ca_cert_dir has been falsified.");
		} else {
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "certificate verify failed", "Stderr from the service-level list command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from the service-level list command when configuration rhsm.ca_cert_dir has been falsified.");
		}
	
		// calling service level list with insecure should now pass
		sshCommandResult = clienttasks.service_level(null,true,null,null,sm_clientUsername,sm_clientPassword, sm_clientOrg, null, true, null, null, null, null);
		
		// assert that option --insecure did NOT persist to rhsm.conf
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "insecure"), "0", "Expected value of "+clienttasks.rhsmConfFile+" server.insecure configuration.  Use of the --insecure option when calling the service-level module should NOT be persisted to rhsm.conf as true.");
	}
	@AfterGroups(value={"ServiceLevelListWithInsecure_Test"},groups={"setup"})
	public void afterServiceLevelListWithInsecure_Test() {
		if (rhsm_ca_cert_dir!=null) clienttasks.config(null, null, true, new String[]{"rhsm","ca_cert_dir",rhsm_ca_cert_dir});
	}
	
	
	
	
	
	

	// Candidates for an automated Test:
	
	
		
	// Configuration methods ***********************************************************************
	@BeforeClass(groups="setup")
	public void createSubscriptionsProvidingProductWithExemptServiceLevels() throws JSONException, Exception {
		String name,productId, serviceLevel, resourcePath;
		List<String> providedProductIds = new ArrayList<String>();
		Map<String,String> attributes = new HashMap<String,String>();
		JSONObject jsonEngProduct, jsonMktProduct, jsonSubscription;
		if (server==null) {
			log.warning("Skipping createSubscriptionsWithVariationsOnProductAttributeSockets() when server is null.");
			return;	
		}
	
		// Subscription with an exempt_support_level
		serviceLevel = "Exempt SLA";
		name = "An \""+serviceLevel+"\" service level subscription (matches all service levels)";
		productId = "exempt-sla-product-sku";
		providedProductIds.clear();
		providedProductIds.add("99000");
		attributes.clear();
		attributes.put("support_level", serviceLevel);
		attributes.put("support_level_exempt", "true");
		attributes.put("version", "0.1");
		//attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "45");
		// delete already existing subscription and products
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productId);
		resourcePath = "/products/"+productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		resourcePath = "/products/"+providedProductIds.get(0);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new engineering product, marketing product that provides the engineering product, and a subscription for the marketing product
		attributes.put("type", "SVC");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, "Exempt Product Bits", providedProductIds.get(0), 1, attributes, null);
		attributes.put("type", "MKT");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name, productId, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), productId, providedProductIds, null);
		// now install the product certificate
		resourcePath = "/products/"+providedProductIds.get(0);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		JSONObject jsonProductCert = new JSONObject (CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath+"/certificate"));
		String cert = jsonProductCert.getString("cert");
		String key = jsonProductCert.getString("key");
		client.runCommandAndWait("echo \""+cert+"\" > "+clienttasks.productCertDir+"/TestExemptProduct"+providedProductIds.get(0)+"_.pem");
		
		// add this exempt service level to the script parameter for exempt service levels
		if (!sm_exemptServiceLevelsInUpperCase.contains(serviceLevel.toUpperCase())) {
			sm_exemptServiceLevelsInUpperCase.add(serviceLevel.toUpperCase());
		}
	}

	
	@BeforeClass(groups="setup")
	public void createMultipleSubscriptionsWithSameServiceLevels() throws JSONException, Exception {
		String name,productId, serviceLevel, resourcePath;
		List<String> providedProductIds = new ArrayList<String>();
		Map<String,String> attributes = new HashMap<String,String>();
		JSONObject jsonEngProduct, jsonMktProduct, jsonSubscription;
		if (server==null) {
			log.warning("Skipping createSubscriptionsWithDuplicateServiceLevels() when server is null.");
			return;	
		}
	
		// Subscription with "Ultimate SLA" support_level
		serviceLevel = "Ultimate SLA";
		name = "The \""+serviceLevel+"\" service level subscription";
		productId = "ultimate-sla-product-sku-1";
		providedProductIds.clear();
		attributes.clear();
		attributes.put("support_level", serviceLevel);
		attributes.put("support_level_exempt", "false");
		attributes.put("version", "0.1");
		//attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "5");
		// delete already existing subscription
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productId);
		resourcePath = "/products/"+productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new marketing product, and a subscription for the marketing product
		attributes.put("type", "MKT");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name, productId, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), productId, providedProductIds, null);

		// Subscription with "ultimate sla" support_level
		serviceLevel = "ultimate sla";
		name = "The \""+serviceLevel+"\" service level subscription";
		productId = "ultimate-sla-product-sku-2";
		providedProductIds.clear();
		attributes.clear();
		attributes.put("support_level", serviceLevel);
		attributes.put("support_level_exempt", "false");
		attributes.put("version", "0.2");
		//attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "5");
		// delete already existing subscription
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productId);
		resourcePath = "/products/"+productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new marketing product, and a subscription for the marketing product
		attributes.put("type", "MKT");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name, productId, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), productId, providedProductIds, null);
	}
	
	
	// Protected methods ***********************************************************************



	
	// Data Providers ***********************************************************************
	

	@DataProvider(name="getExemptInstalledProductAndServiceLevelData")
	public Object[][] getExemptInstalledProductAndServiceLevelDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getExemptInstalledProductAndServiceLevelDataAsListOfLists());
	}
	/**
	 * @return List of [Object bugzilla, String installedProductId, String serviceLevel]
	 */
	protected List<List<Object>>getExemptInstalledProductAndServiceLevelDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
	
		// get the available service levels
		List<String> serviceLevels = new ArrayList<String>();
		for (List<Object> availableServiceLevelData : getAllAvailableServiceLevelDataAsListOfLists()) {
			// availableServiceLevelData :   Object bugzilla, String serviceLevel
			serviceLevels.add((String)availableServiceLevelData.get(1));
		}
		
		// get all of the installed products
		List<ProductCert> installedProductCerts = clienttasks.getCurrentProductCerts();
		
		// process the available SubscriptionPools looking for provided productIds from a pool that has an exempt service level
		for (List<Object> subscriptionPoolsData : getAvailableSubscriptionPoolsDataAsListOfLists()) {
			SubscriptionPool subscriptionPool = (SubscriptionPool) subscriptionPoolsData.get(0);

			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+subscriptionPool.poolId));	
			JSONArray jsonProvidedProducts = jsonPool.getJSONArray("providedProducts");
			String value = CandlepinTasks.getPoolProductAttributeValue(jsonPool, "support_level_exempt");
			
			// skip this subscriptionPool when "support_level_exempt" productAttribute is NOT true
			if (value==null) continue;
			if (!Boolean.valueOf(value)) continue;
			
			// process each of the provided products (exempt) to see if it is installed
			for (int i = 0; i < jsonProvidedProducts.length(); i++) {
				JSONObject jsonProvidedProduct = (JSONObject) jsonProvidedProducts.get(i);
				String productId = jsonProvidedProduct.getString("productId");
				
				// is productId installed?
				if (ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", productId, installedProductCerts)!=null) {
					// found an installed exempt product, add a row for each of the service levels
					for (String serviceLevel : serviceLevels) {
						// Object bugzilla, String installedProductId, String serviceLevel
						ll.add(Arrays.asList(new Object[]{null, productId, serviceLevel}));
					}
				}
			}
		}
		
		return ll;
	}
}
