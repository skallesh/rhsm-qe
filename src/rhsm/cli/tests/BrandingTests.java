package rhsm.cli.tests;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductCert;
import rhsm.data.ProductNamespace;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author jsefler
 * 
 * References
 *  Design Doc: https://mojo.redhat.com/docs/DOC-186259
 *  Dev Test Doc: https://mojo.redhat.com/docs/DOC-21827
 *  TCMS plan link https://tcms.engineering.redhat.com/plan/11066/flex-branding-test-cases
 *  
 * Brandbot is owned by the initscripts package and developed by Bill Nottingham
 * [root@jsefler-7 ~]# rpm -ql initscripts | grep brandbot
 * /usr/lib/systemd/system/brandbot.path
 * /usr/lib/systemd/system/brandbot.service
 * /usr/lib/systemd/system/multi-user.target.wants/brandbot.path
 * /usr/sbin/brandbot
 * 
 * Be wary of this error when trying to update the branding file too fast
 * [root@jsefler-7 ~]# tail -f /var/log/messages | grep brandbot
 * Dec  4 14:23:59 jsefler-7 systemd: brandbot.service start request repeated too quickly, refusing to start.
 * Dec  4 14:23:59 jsefler-7 systemd: Unit brandbot.service entered failed state.
 */
@Test(groups = {"BrandingTests","Tier2Tests"})
public class BrandingTests extends SubscriptionManagerCLITestScript {
	
	@Test(	description="assert that brandbot service is running",
			groups={"AcceptanceTests","Tier1Tests"},
			enabled=false)	// TODO not sure how this works... the status of this service is inactive, yet it appears to be automatically started/stopped as needed NEEDINFO from notting
	public void BrandbotServiceShouldBeRunning_Test() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)<7) throw new SkipException("Brandbot is an initscripts tool feature of Flexible Branding in RHEL7.");	// initscripts-9.49.17-1.el7
		RemoteFileTasks.runCommandAndAssert(client, "systemctl is-active brandbot.service", Integer.valueOf(0), "^active$", null);
	}
	
	
	@Test(	description="incrementally attach all available subscriptions and verify tests for Flexible Branding",
			groups={"AttachSubscriptionsForFlexibleBranding_Test","AcceptanceTests","Tier1Tests","blockedByBug-884290"},
			priority=100,
			enabled=true)
	public void AttachSubscriptionsForFlexibleBranding_Test() throws JSONException, Exception {
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null,(List<String>)null, null, null, null, true, false, null, null, null);
		
		// we will start out by removing the current brand name.
		sleep(5000); // before echo to avoid tail -f /var/log/messages | grep brandbot...   systemd: brandbot.service start request repeated too quickly, refusing to start.
		client.runCommandAndWait("rm -f "+brandingFile);
		
		// choose subscription pools for FlexibleBranded tests from among the available subscription pools
		List<SubscriptionPool> subscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool brandedSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", brandedSubscriptionProductId, subscriptionPools);
		subscriptionPools = getRandomSubsetOfList(subscriptionPools,10);	// randomly reduce the number of subscriptionPools tested
		
		// include the branded subscription pool for brandedSubscriptionProductId at the head of subscriptionPools
		if (brandedSubscriptionPool!=null) subscriptionPools.add(0, brandedSubscriptionPool);
		
		// loop through subscription pools and assert flexible branding tests after attaching each subscription
		boolean flexibleBrandedSubscriptionsFound = false;
		for (SubscriptionPool pool : subscriptionPools) {
// TODO this should not matter			if (CandlepinTasks.isPoolAModifier(sm_clientUsername, sm_clientPassword, pool.poolId, sm_serverUrl)) continue; // skip modifier pools
			String brandNameBeforeSubscribing = getCurrentBrandName();
			String brandNameStatBeforeSubscribing = getCurrentBrandNameFileStat();
			String prettyNameBeforeSubscribing = getCurrentPrettyName();
			log.info("Currently, the flexible brand name prior to subscribing to pool '"+pool.subscriptionName+"' is '"+brandNameBeforeSubscribing+"'.");
			clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null);
			Boolean verifiedSystemsExpectedBrandedNameAfterEvent = verifySystemsExpectedBrandedNameAfterEvent(brandNameBeforeSubscribing,brandNameStatBeforeSubscribing,prettyNameBeforeSubscribing,"subscribing to pool '"+pool.subscriptionName+"'");
			if (verifiedSystemsExpectedBrandedNameAfterEvent==null) {
				// unentitle the system to clear the undefined case of multiple installed OS products
				//clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null);
				RemoveSubscriptionsForFlexibleBranding_Test();
			} else if (verifiedSystemsExpectedBrandedNameAfterEvent) flexibleBrandedSubscriptionsFound=true; 
		}
		
		// throw SkipException when no flexible branding was tested
		if (!flexibleBrandedSubscriptionsFound) throw new SkipException("No branding subscriptions were found among the available subscriptions that will brand one of the currently installed OS products.");
	}
	
	
	@Test(	description="incrementally remove attached subscriptions and verify tests for Flexible Branding",
			//depend on priority instead of dependsOnMethods={"AttachSubscriptionsForFlexibleBranding_Test"},
			priority=101,
			groups={"AcceptanceTests","Tier1Tests"},
			enabled=true)
	public void RemoveSubscriptionsForFlexibleBranding_Test() {
		
		// loop through all the attached subscriptions and remove them while running tests for branding
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			String brandNameBeforeUnsubscribing = getCurrentBrandName();
			String brandNameStatBeforeUnsubscribing = getCurrentBrandNameFileStat();
			String prettyNameBeforeUnsubscribing = getCurrentPrettyName();
			log.info("The flexible brand name prior to unsubscribing from subscription '"+productSubscription.productName+"' is currently '"+brandNameBeforeUnsubscribing+"'.");
			clienttasks.unsubscribe_(null,productSubscription.serialNumber, null, null, null);
			verifySystemsExpectedBrandedNameAfterEvent(brandNameBeforeUnsubscribing,brandNameStatBeforeUnsubscribing,prettyNameBeforeUnsubscribing,"unsubscribing from '"+productSubscription.productName+"'");
		}
	}
	
	
	@Test(	description="autosubscribe and verify tests for Flexible Branding",
			dependsOnGroups={},
			priority=200,
			groups={"AcceptanceTests","Tier1Tests","AutoSubscribeForFlexibleBranding_Test"},
			enabled=true)
	public void AutoSubscribeForFlexibleBranding_Test() {
		// we will start out by unregistering and removing the current brand name.
		clienttasks.unregister(null, null, null);
		client.runCommandAndWait("rm -f "+brandingFile);
		
		String brandNameBeforeRegisteringWithAutosubscribe = getCurrentBrandName();
		String brandNameStatBeforeRegisteringWithAutosubscribe = getCurrentBrandNameFileStat();
		String prettyNameBeforeRegisteringWithAutosubscribe = getCurrentPrettyName();
		log.info("Currently, the flexible brand name prior to registering with autosubscribe is '"+brandNameBeforeRegisteringWithAutosubscribe+"'.");

		// register with autosubscribe
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null,(List<String>)null, null, null, null, null, null, null, null, null);
		
		// tests for branding
		Boolean verifiedSystemsExpectedBrandedNameAfterEvent = verifySystemsExpectedBrandedNameAfterEvent(brandNameBeforeRegisteringWithAutosubscribe,brandNameStatBeforeRegisteringWithAutosubscribe,prettyNameBeforeRegisteringWithAutosubscribe,"registering with autosubscribe");
		if (verifiedSystemsExpectedBrandedNameAfterEvent==null || !verifiedSystemsExpectedBrandedNameAfterEvent) {
			throw new SkipException("Could not verify the systems expected branded name after registering with autosubscribe.");
		}
	}
	
	
	@Test(	description="run an rhsmcertd event and verify tests for Flexible Branding",
			//depend on priority instead of dependsOnMethods={"AutoSubscribeForFlexibleBranding_Test"},
			priority=201,
			groups={"AcceptanceTests","Tier1Tests","blockedByBug-1038664"},
			enabled=true)
	public void RhsmcertdCheckForFlexibleBranding_Test() {
		// we will start out by removing the current brand name and the current entitlements.
		clienttasks.removeAllCerts(false, true, false);
		client.runCommandAndWait("rm -f "+brandingFile);

		String brandNameBeforeRunningRhsmcertdCheck = getCurrentBrandName();
		String brandNameStatBeforeRunningRhsmcertdCheck = getCurrentBrandNameFileStat();
		String prettyNameBeforeRunningRhsmcertdCheck = getCurrentPrettyName();
		log.info("Currently, the flexible brand name prior to an rhsmcertd check is '"+brandNameBeforeRunningRhsmcertdCheck+"'.");
		
		// run rhsmcertd
		clienttasks.run_rhsmcertd_worker(false);
		
		// tests for branding
		Boolean verifiedSystemsExpectedBrandedNameAfterEvent = verifySystemsExpectedBrandedNameAfterEvent(brandNameBeforeRunningRhsmcertdCheck,brandNameStatBeforeRunningRhsmcertdCheck,prettyNameBeforeRunningRhsmcertdCheck,"running the rhsmcertd-worker check");
		if (verifiedSystemsExpectedBrandedNameAfterEvent==null || !verifiedSystemsExpectedBrandedNameAfterEvent) {
			throw new SkipException("Could not verify the systems expected branded name after running the rhsmcertd-worker to restore the consumer's entitlements from the prior AutoSubscribeForFlexibleBranding_Test.");
		}
	}
	
	
	@Test(	description="run an rhsmcertd healing event and verify tests for Flexible Branding",
			//depend on priority instead of dependsOnMethods={"AutoSubscribeForFlexibleBranding_Test"},
			priority=202,
			groups={"AcceptanceTests","Tier1Tests"},
			enabled=true)
	public void RhsmcertdHealingUpdateForFlexibleBranding_Test() {
		// we will start out by removing the current brand name and removing the current entitlements.
		clienttasks.unsubscribe(true, (BigInteger)null, null, null, null);
		client.runCommandAndWait("rm -f "+brandingFile);

		String brandNameBeforeRunningRhsmcertdHealCheck = getCurrentBrandName();
		String brandNameStatBeforeRunningRhsmcertdHealCheck = getCurrentBrandNameFileStat();
		String prettyNameBeforeRunningRhsmcertdHealCheck = getCurrentPrettyName();
		log.info("Currently, the flexible brand name prior to an rhsmcertd heal check is '"+brandNameBeforeRunningRhsmcertdHealCheck+"'.");
		
		// make sure autoheal is enabled
		clienttasks.autoheal(null, true, null, null, null, null);
		
		// run rhsmcertd check for healing
		clienttasks.run_rhsmcertd_worker(true);
		
		// tests for branding
		Boolean verifiedSystemsExpectedBrandedNameAfterEvent = verifySystemsExpectedBrandedNameAfterEvent(brandNameBeforeRunningRhsmcertdHealCheck,brandNameStatBeforeRunningRhsmcertdHealCheck,prettyNameBeforeRunningRhsmcertdHealCheck,"running the rhsmcertd-worker healing check");
		if (verifiedSystemsExpectedBrandedNameAfterEvent==null || !verifiedSystemsExpectedBrandedNameAfterEvent) {
			throw new SkipException("Could not verify the systems expected branded name after running the rhsmcertd-worker with healing.");
		}
	}
	
	
	@Test(	description="assert that brandbot only reads the first line of the branding file",
			groups={"blockedByBug-1031490"},
			enabled=true)
	public void BrandbotShouldHandleMultiLineBrandingFile_Test() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)<7) throw new SkipException("Brandbot is an initscripts tool feature of Flexible Branding in RHEL7.");	// initscripts-9.49.17-1.el7
		
		String actualBrandName,actualPrettyName;
		log.info("Testing a single line branding file...");
		sleep(5000); // before echo to avoid tail -f /var/log/messages | grep brandbot...   systemd: brandbot.service start request repeated too quickly, refusing to start.
		RemoteFileTasks.runCommandAndAssert(client, "echo 'RHEL/Branded OS (line 1)' > "+brandingFile, 0);
		actualBrandName = getCurrentBrandName();
		Assert.assertEquals(actualBrandName, "RHEL/Branded OS (line 1)", "The brand name contained within the first line of the brand file '"+brandingFile+"'.");
		actualPrettyName = getCurrentPrettyName();
		Assert.assertEquals(actualPrettyName, "RHEL/Branded OS (line 1)", "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"'.");

		log.info("Testing a multi-line branding file...");
		sleep(5000); // before echo to avoid tail -f /var/log/messages | grep brandbot...   systemd: brandbot.service start request repeated too quickly, refusing to start.
		RemoteFileTasks.runCommandAndAssert(client, "echo 'RHEL/SubBranded OS (line 2)' >> "+brandingFile, 0);
		actualBrandName = getCurrentBrandName();
		Assert.assertEquals(actualBrandName, "RHEL/Branded OS (line 1)", "The brand name contained within the first line of the brand file '"+brandingFile+"'.");
		actualPrettyName = getCurrentPrettyName();
		Assert.assertEquals(actualPrettyName, "RHEL/Branded OS (line 1)", "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' (Should not contain any new line characters).");
	}
	
	
	@Test(	description="assert that brandbot trims white space from the first line of the branding file",
			groups={},
			enabled=true)
	public void BrandbotShouldTrimWhiteSpaceFromBrandingFile_Test() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)<7) throw new SkipException("Brandbot is an initscripts tool feature of Flexible Branding in RHEL7.");	// initscripts-9.49.17-1.el7
		
		String actualBrandName,actualPrettyName;
		log.info("Testing a single line branding file with leading and trailing white space...");
		sleep(5000); // before echo to avoid tail -f /var/log/messages | grep brandbot...   systemd: brandbot.service start request repeated too quickly, refusing to start.
		RemoteFileTasks.runCommandAndAssert(client, "echo '  RHEL-x.y Branded OS  ' > "+brandingFile, 0);
		actualBrandName = getCurrentBrandName();
		Assert.assertEquals(actualBrandName, "  RHEL-x.y Branded OS  ", "The brand name contained within the first line of the brand file '"+brandingFile+"' (should contain leading and trailing white space).");
		actualPrettyName = getCurrentPrettyName();
		Assert.assertEquals(actualPrettyName, "RHEL-x.y Branded OS", "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' (should NOT contain leading nor trailing white space).");
	}
	
	
	@Test(	description="assert that brandbot does nothing when the brand file is removed",	//  Brandbot SHOULD handle /var/lib/rhsm/branded_name not existing.
			groups={},
			enabled=true)
	public void BrandbotShouldHandleNonExistantBrandingFile_Test() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)<7) throw new SkipException("Brandbot is an initscripts tool feature of Flexible Branding in RHEL7.");	// initscripts-9.49.17-1.el7
		
		//	> If /var/lib/rhsm/branded_name is removed, what should PRETTY_NAME be? In
		//	> initscripts-9.49.11-1.el7.x86_64 it retains its prior value before the
		//	> branded_name was removed.  I don't know if that is correct.
		//
		//	(The answer is likely 'whatever PM wants to happen'. But aside from that...)
		//	Bill
				
		String actualBrandName,actualPrettyName;
		log.info("Testing a single line branding to make sure PRETTY_NAME exists before testing the non-existant branding file...");
		sleep(5000); // before echo to avoid tail -f /var/log/messages | grep brandbot...   systemd: brandbot.service start request repeated too quickly, refusing to start.
		RemoteFileTasks.runCommandAndAssert(client, "echo 'RHEL Branded OS' > "+brandingFile, 0);
		actualBrandName = getCurrentBrandName();
		Assert.assertEquals(actualBrandName, "RHEL Branded OS", "The brand name contained within the first line of the brand file '"+brandingFile+"'.");
		actualPrettyName = getCurrentPrettyName();
		Assert.assertEquals(actualPrettyName, "RHEL Branded OS", "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"'.");
		
		log.info("Testing a non-existant branding file...");
		sleep(5000); // before echo to avoid tail -f /var/log/messages | grep brandbot...   systemd: brandbot.service start request repeated too quickly, refusing to start.
		RemoteFileTasks.runCommandAndAssert(client, "rm -f "+brandingFile, 0);
		Assert.assertTrue(!RemoteFileTasks.testExists(client, brandingFile), "The brand file '"+brandingFile+"' should not exist for this test.");
		actualBrandName = getCurrentBrandName();
		Assert.assertNull(actualBrandName, "The brand name is not defined when the brand file '"+brandingFile+"' does not exist.");
		Assert.assertEquals(getCurrentPrettyName(),actualPrettyName, "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' (should remain unchanged when the brand file is removed).");
	}
	
	
	@Test(	description="assert that brandbot removes PRETTY_NAME when the first line of the branding file is empty",	//  Brandbot SHOULD handle /var/lib/rhsm/branded_name being empty.
			groups={"blockedByBug-1031490"},
			enabled=true)
	public void BrandbotShouldHandleEmptyBrandingFile_Test() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)<7) throw new SkipException("Brandbot is an initscripts tool feature of Flexible Branding in RHEL7.");	// initscripts-9.49.17-1.el7
		
		//	> If /var/lib/rhsm/branded_name is an empty file, what should PRETTY_NAME
		//	> be?  In initscripts-9.49.11-1.el7.x86_64 PRETTY_NAME is removed from
		//	> /etc/os-release.  I think it should be PRETTY_NAME = "".
		//	>
		//	> What is the consequence of PRETTY_NAME missing from /etc/os-release?
		//
		//	Logical behaviors would be:
		//
		//	1) removing PRETTY_NAME
		//	2) reverting it to the unbranded name
		//
		//	Since we don't (AFAIK) have the data to do #2, #1 makes sense to me.
		//
		//	Bill
		
		String actualBrandName,actualPrettyName;
		log.info("Testing an empty branding file...");
		sleep(5000); // before echo to avoid tail -f /var/log/messages | grep brandbot...   systemd: brandbot.service start request repeated too quickly, refusing to start.
		RemoteFileTasks.runCommandAndAssert(client, "rm -f "+brandingFile+" && touch "+brandingFile, 0);
		actualBrandName = getCurrentBrandName();
		Assert.assertEquals(actualBrandName, "", "The brand name contained within the first line of the brand file '"+brandingFile+"' (should be an empty string).");
		actualPrettyName = getCurrentPrettyName();
		Assert.assertNull(actualPrettyName, "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' (should NOT be present when the brand file is empty).");
		
		log.info("Testing a non-empty branding file, but the first line is empty...");
		sleep(5000); // before echo to avoid tail -f /var/log/messages | grep brandbot...   systemd: brandbot.service start request repeated too quickly, refusing to start.
		RemoteFileTasks.runCommandAndAssert(client, "echo '' >> "+brandingFile, 0);
		RemoteFileTasks.runCommandAndAssert(client, "echo 'RHEL SubBranded OS (second line)' >> "+brandingFile, 0);
		actualBrandName = getCurrentBrandName();
		Assert.assertEquals(actualBrandName, "", "The brand name contained within the first line of the brand file '"+brandingFile+"' (should be an empty string).");
		actualPrettyName = getCurrentPrettyName();
		Assert.assertNull(actualPrettyName, "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' (should NOT be present when the first line of the brand file is empty).");
	}
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	
	// Protected Methods ***********************************************************************
	protected final String brandingFile = "/var/lib/rhsm/branded_name";
	protected final String osReleaseFile = "/etc/os-release";
	protected final String brandedSubscriptionProductId = "branded-generic-os-sku";
	
	/**
	 * @param entitlementCerts TODO
	 * @return the eligible brand names based on the current entitlement certs and the currently installed product certs
	 */
	Set<String> getEligibleBrandNamesFromCurrentEntitlementCerts() {
		
		// Rules:
		//  - eligible brand names come from the currently entitled productNamespaces and are identified by a brandType = "OS"
		//  - the corresponding productId must be among the currently installed product certs to be eligible
		
		Set<String> eligibleBrandNamesSet = new HashSet<String>();
		List<String> eligibleBrandNamesList = new ArrayList<String>();
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
			for (ProductNamespace productNamespace : entitlementCert.productNamespaces) {
				if (ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", productNamespace.id, currentProductCerts) != null) {
					if (productNamespace.brandType != null) {
						if (productNamespace.brandType.equals("OS")) {
							/* THE ORIGINAL FLEX BRANDING IMPLEMENTATION DUAL-PURPOSED THE PRODUCT NAME AS THE SOURCE OF THE BRANDING NAME
							eligibleBrandNamesList.add(productNamespace.name);
							eligibleBrandNamesSet.add(productNamespace.name);
							*/
							eligibleBrandNamesList.add(productNamespace.brandName);
							eligibleBrandNamesSet.add(productNamespace.brandName);
						}
					}
				}
			}
		}
		if (eligibleBrandNamesList.size() > eligibleBrandNamesSet.size()) {
			log.warning("Currently there are multiple entitled OS brand products by the same name "+eligibleBrandNamesList+".  This can happen when multiple OS subscriptions have been stacked.");
		}
		return eligibleBrandNamesSet;
	}
	
	/**
	 * @return the first line contained within the branding file; null if the branding file does not exist
	 */
	String getCurrentBrandName() {
		if (!RemoteFileTasks.testExists(client, brandingFile)) return null;
		
		// Rules:
		//  - the current brand name should only be the first line of the brand file (Bug 1031490)
		
		//String brandName = client.runCommandAndWait("head -1 "+brandNameFile).getStdout().trim();
		String brandName = client.runCommandAndWait("cat "+brandingFile).getStdout();
		brandName = brandName.replaceAll("\n.*", "");	// strip off all secondary lines
		return brandName;
	}
	
	/**
	 * @return a string representing the creation/modification/change time of the branding file
	 */
	String getCurrentBrandNameFileStat() {
		return client.runCommandAndWait("stat -c 'File= %n Created= %w Modified= %y Changed= %z' "+brandingFile).getStdout().trim();
	}
	
	/**
	 * @return the value of PRETTY_NAME from /etc/os-release which is under the control of /usr/lib/systemd/system/brandbot.service (owned by package initscripts); null if not found
	 */
	String getCurrentPrettyName() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)<7) {
			log.warning("Brandbot and the setting of PRETTY_NAME in '"+osReleaseFile+"' is a feature of Flexible Branding in RHEL7.  Skipping the retrieval of PRETTY_NAME."); 
			return null;
		}
		//	[root@jsefler-7 ~]# cat /etc/os-release
		//	NAME="Red Hat Enterprise Linux Server"
		//	VERSION="7.0 (Maipo)"
		//	ID="rhel"
		//	VERSION_ID="7.0"
		//	PRETTY_NAME="Red Hat Enterprise Linux Server 7.0 (Maipo)"
		//	ANSI_COLOR="0;31"
		//	CPE_NAME="cpe:/o:redhat:enterprise_linux:7.0:beta:server"
		//
		//	REDHAT_BUGZILLA_PRODUCT="Red Hat Enterprise Linux 7"
		//	REDHAT_BUGZILLA_PRODUCT_VERSION=7.0
		//	REDHAT_SUPPORT_PRODUCT="Red Hat Enterprise Linux"
		//	REDHAT_SUPPORT_PRODUCT_VERSION=7.0
		
		String osReleaseContents = client.runCommandAndWait("cat "+osReleaseFile).getStdout().trim();
		String prettyNameRegex = "(PRETTY_NAME\\s*=\\s*(\\\"(.*?(\\n.*?)*)\\\"))|(PRETTY_NAME\\s*=\\s*\\w+)";
		List<String> prettyNameMatches = getSubstringMatches(osReleaseContents, prettyNameRegex);
		if (prettyNameMatches.isEmpty()) return null;
		if (prettyNameMatches.size()>1) Assert.fail("Encountered more than one PRETTY_NAME in "+osReleaseFile);	// this should not happen
		String prettyName = prettyNameMatches.get(0);
		prettyName = prettyName.replaceFirst("PRETTY_NAME\\s*=\\s*", "");	// strip off PRETTY_NAME =
		prettyName = prettyName.replaceAll("(^\"|\"$)", "");	// strip off surrounding double quotes
		return prettyName;
	}
	

	/**
	 * This routine performs the expected assertions based on the current entitlements after an event has occurred.
	 * It will assert the contents of the branding file and the PRETTY_NAME in /etc/os-release. 
	 * An event like subscribe or unsubscribe should be called immediately before this verification method is called.
	 * @param brandNameBeforeEvent - value from getCurrentBrandName() before the event is called
	 * @param brandNameStatBeforeEvent - value from getCurrentBrandNameFileStat() before the event is called
	 * @param prettyNameBeforeEvent - value from getCurrentPrettyName() before the event is called
	 * @param afterEventDescription - a description of the event like "after subscribing to pool 'Awesome OS'."
	 * @param entitlementCerts TODO
	 * @return - true or false indicates when a flexible branded event was actually asserted; null is returned when the system is entitled to more than one installed OS product (an undefined case).
	 */
	protected Boolean verifySystemsExpectedBrandedNameAfterEvent(String brandNameBeforeEvent, String brandNameStatBeforeEvent, String prettyNameBeforeEvent, String afterEventDescription) { 
		
		boolean flexibleBrandedSubscriptionsFound = false;
		Set<String> eligibleBrandNames = getEligibleBrandNamesFromCurrentEntitlementCerts();
		
		// determine the expected brand name after subscribing
		// Rules:
		//  - eligible brand names come from the currently entitled productNamespaces
		//  - the corresponding productId must be among the currently installed product certs to be eligible
		//  - if more than one brand name is eligible, no update is made to the branding file
		//  - if only one brand name is eligible and it does NOT already equal the current brand name, then an update is made
		//  - if no brand name is eligible, no update is made
		//  - updates are sought out when entitlements are refreshed
		// 
		// Design Doc: https://mojo.redhat.com/docs/DOC-186259
		// Developer Test Notes: https://mojo.redhat.com/docs/DOC-21827
		// Revised/Enhancements Design March 2014: https://engineering.redhat.com/trac/Entitlement/wiki/FlexibleBranding
		String expectedBrandNameAfterSubscribing=null;
		if (eligibleBrandNames.size()>1) {
 			log.warning("Currently there are multiple eligible brand names "+eligibleBrandNames+" which means that more than one OS product is installed and entitled.  Skipping brand name assertion because this is not a valid scenario and the expected brand name is undefined.");
			return null;
		}
		if (eligibleBrandNames.isEmpty()) {
			log.warning("There are no eligible brand names based on the currently attached entitlements, therefore the actual brand name should remain unchanged.");
			expectedBrandNameAfterSubscribing = brandNameBeforeEvent;
		}
		if (eligibleBrandNames.size()==1) {
			expectedBrandNameAfterSubscribing = (String) eligibleBrandNames.toArray()[0];
			log.info("There is only one eligible brand name based on the currently attached entitlements, therefore the actual brand name should be '"+expectedBrandNameAfterSubscribing+"'.");
			flexibleBrandedSubscriptionsFound=true;
		}
	
		// verify the actualBrandNameAfterSubscribing = expectedBrandNameAfterSubscribing
		String actualBrandNameAfterSubscribing = getCurrentBrandName();
		Assert.assertEquals(actualBrandNameAfterSubscribing, expectedBrandNameAfterSubscribing, "The brand name contained within the first line of the brand file '"+brandingFile+"' after "+afterEventDescription);
		
		// verify that the brand file was NOT altered when the expectedBrandNameAfterSubscribing = brandNameBeforeSubscribing
		if (expectedBrandNameAfterSubscribing!=null && expectedBrandNameAfterSubscribing.equals(brandNameBeforeEvent)) {
			String brandNameStatAfterSubscribing = getCurrentBrandNameFileStat();
			Assert.assertEquals(brandNameStatAfterSubscribing, brandNameStatBeforeEvent, "After "+afterEventDescription+", if the brand name should remain unchanged, then the modification and change stats on the brand file '"+brandingFile+"' should remain unaltered.");
		}
		if (expectedBrandNameAfterSubscribing!=null && !expectedBrandNameAfterSubscribing.equals(brandNameBeforeEvent)) {
			String brandNameStatAfterSubscribing = getCurrentBrandNameFileStat();
			Assert.assertTrue(!brandNameStatAfterSubscribing.equals(brandNameStatBeforeEvent), "After "+afterEventDescription+", if the brand name should change, then the modification/change stats on the brand file '"+brandingFile+"' should obviously change too.");
		}
		
		// verify that /usr/sbin/brandbot has updated PRETTY_NAME in /etc/os-release
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) {
			String actualPrettyNameAfterSubscribing = getCurrentPrettyName();
			if (expectedBrandNameAfterSubscribing!=null) {
				if (expectedBrandNameAfterSubscribing.isEmpty()) {
					// see BrandbotShouldHandleEmptyBrandingFile_Test
					Assert.assertNull(actualPrettyNameAfterSubscribing, "The PRETTY_NAME in '"+osReleaseFile+"' governed by /usr/lib/systemd/system/brandbot.service should be removed when the first line of the brand file '"+brandingFile+"' is empty after "+afterEventDescription+".");
				} else {
					Assert.assertEquals(actualPrettyNameAfterSubscribing, expectedBrandNameAfterSubscribing, "The PRETTY_NAME in '"+osReleaseFile+"' governed by /usr/lib/systemd/system/brandbot.service should match the first line of the brand file '"+brandingFile+"' after "+afterEventDescription+".");
				}
			} else {
				// see BrandbotShouldHandleNonExistantBrandingFile_Test
				Assert.assertEquals(actualPrettyNameAfterSubscribing,prettyNameBeforeEvent, "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' should remain unchanged when the expected brand name is null after "+afterEventDescription+".");
			}
		} else log.warning("Brandbot and the setting of PRETTY_NAME in '"+osReleaseFile+"' is a feature of Flexible Branding in RHEL7.  Skipping assertions for expected PRETTY_NAME in '"+clienttasks.redhatRelease+"'.");
		
		return flexibleBrandedSubscriptionsFound;
	}
	
	
	// Data Providers ***********************************************************************
	
	
	
	// Configuration Methods ***********************************************************************
	@BeforeClass(groups="setup")
	public void createProductsAndSubscriptionWithBrandingBeforeClass() throws JSONException, Exception {
		
		if (server==null) {
			log.warning("Skipping createProductsAndSubscriptionWithBrandingBeforeClass() when server is null.");
			return;	
		}
		
		// Subscription with an branding
		String productId = brandedSubscriptionProductId;
		List<String> providedProductIds = new ArrayList<String>();
		providedProductIds.add("99010");
		providedProductIds.add("99020");
		providedProductIds.add("99030");
		providedProductIds.add("99040");
		
		// delete already existing subscription, marketing product, and provided engineering products
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productId);
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "/products/"+productId);
		for (String providedProductId : providedProductIds)	CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "/products/"+providedProductId);
		
		// create new engineering products, a marketing product that provides the engineering products, and a subscription for the marketing product
		Map<String,String> attributes = new HashMap<String,String>();
		attributes.put("type", "SVC");
		//attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("version", "1.0.0");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "Generic non-OS Product V"+attributes.get("version"), "99010", 1, attributes, null);
		attributes.put("version", "2.0.0");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "Generic OSX Product V"+attributes.get("version"), "99020", 1, attributes, null);
		attributes.put("version", "3.0.0");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "Generic OS Product V"+attributes.get("version"), "99030", 1, attributes, null);
		attributes.put("version", "4.0.0");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "Generic OS Product V"+attributes.get("version"), "99040", 1, attributes, null);
		attributes.put("type", "MKT");
		attributes.put("version", "1.0");
		attributes.put("support_level", "Generic SLA");
		attributes.put("support_level_exempt", "false");
		attributes.put("multi-entitlement", "yes");
		attributes.put("warning_period", "90");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "Branded subscription for Generic Products", productId, 1, attributes, null);
		List<Map<String,String>> brandingMaps = new ArrayList<Map<String,String>>();
		//brandingMaps.add(new HashMap<String,String>(){{put("productId","99010");put("type",null);put("name",null);}});	// Runtime Error not-null property references a null or transient value: org.candlepin.model.Branding.name at org.hibernate.engine.internal.Nullability.checkNullability:103
		//brandingMaps.add(new HashMap<String,String>(){{put("productId","99010");put("type","");put("name","");}});	// Runtime Error not-null property references a null or transient value: org.candlepin.model.Branding.name at org.hibernate.engine.internal.Nullability.checkNullability:103
		brandingMaps.add(new HashMap<String,String>(){{put("productId","99010");put("type"," ");put("name"," ");}});
		brandingMaps.add(new HashMap<String,String>(){{put("productId","99020");put("type","OSX");put("name","Branded Generic OSX Product V20");}});
		brandingMaps.add(new HashMap<String,String>(){{put("productId","99030");put("type","OS");put("name","Branded Generic OS Product V30");}});
		brandingMaps.add(new HashMap<String,String>(){{put("productId","99040");put("type","OS");put("name","Branded Generic OS Product V40");}});
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 15, -1*24*60/*1 day ago*/, 30*24*60/*30 days from now*/, getRandInt(), getRandInt(), productId, providedProductIds, brandingMaps);
		
		// now install the engineering product certificates
		for (String providedProductId : providedProductIds) {
			JSONObject jsonProduct = new JSONObject (CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "/products/"+providedProductId));
			JSONObject jsonProductCert = new JSONObject (CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "/products/"+providedProductId+"/certificate"));
			String cert = jsonProductCert.getString("cert");
			String key = jsonProductCert.getString("key");
			String installedProductCertFilename = (/*jsonProduct.getString("name")*/"GenericProduct"+" "+jsonProduct.getString("id")+"_.pem").replaceAll(" ", "_");
			client.runCommandAndWait("rm -f "+clienttasks.productCertDir+"/"+installedProductCertFilename);
//			if (providedProductId.equals("99030") && randomGenerator.nextBoolean()) continue;	// randomly skip the install of 99030
			if (providedProductId.equals("99030")) continue;	// skip the install of 99030
			client.runCommandAndWait("echo \""+cert+"\" > "+clienttasks.productCertDir+"/"+installedProductCertFilename);
		}
	}
	
	@BeforeClass(groups="setup",dependsOnMethods={"createProductsAndSubscriptionWithBrandingBeforeClass"})	// continue to run createProductsAndSubscriptionWithBrandingBeforeClass as a no-break for other Tests.
	public void skipBrandingTestsForOldVerionsBeforeClass() {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.10.14-7") && clienttasks.isPackageVersion("python-rhsm","<","1.10.12-2")) {	// rhel7.0 GA packages
			throw new SkipException("Flexible Branding is a subscription feature available in RHEL7.");
		}
	}
}

