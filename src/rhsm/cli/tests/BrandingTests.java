package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
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
 * Reference Design Doc:
 * https://mojo.redhat.com/docs/DOC-186259
 * 
 * Brandbot is owned by the initscripts package and developed by Bill Nottingham
 * [root@jsefler-7 ~]# rpm -ql initscripts | grep brandbot
 * /usr/lib/systemd/system/brandbot.path
 * /usr/lib/systemd/system/brandbot.service
 * /usr/lib/systemd/system/multi-user.target.wants/brandbot.path
 * /usr/sbin/brandbot
 */
@Test(groups = { "BrandingTests" })
public class BrandingTests extends SubscriptionManagerCLITestScript {
	
	@Test(	description="incrementally attach all available subscriptions and verify tests for Flexible Branding",
			groups={"AttachSubscriptionsForFlexibleBranding_Test"},
			enabled=true)
	public void AttachSubscriptionsForFlexibleBranding_Test() {
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null,(List<String>)null, null, null, null, true, false, null, null, null);
		
		// we will start out by removing the current brand name.
		client.runCommandAndWait("rm -f "+brandingFile);
		 
		// loop through all the available subscription pools searching for FlexibleBranded subscriptions
		boolean flexibleBrandedSubscriptionsFound = false;
		// too time consuming for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
		for (SubscriptionPool pool : getRandomSubsetOfList(clienttasks.getCurrentlyAvailableSubscriptionPools(),10)) {
			String brandNameBeforeSubscribing = getCurrentBrandName();
			String brandNameStatBeforeSubscribing = getCurrentBrandNameFileStat();
			log.info("Currently, the flexible brand name prior to subscribing to pool '"+pool.subscriptionName+"' is '"+brandNameBeforeSubscribing+"'.");
			clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null);
			List<String> eligibleBrandNames = getEligibleBrandNamesFromCurrentEntitlements();
			
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
			String expectedBrandNameAfterSubscribing=null;
			if (eligibleBrandNames.size()>1) {
				log.warning("Currently there are multiple eligible brand names "+eligibleBrandNames+", therefore the actual brand name should remain unchanged.");
				expectedBrandNameAfterSubscribing = brandNameBeforeSubscribing;
				flexibleBrandedSubscriptionsFound=true;
			}
			if (eligibleBrandNames.isEmpty()) {
				log.warning("Currently there are no eligible brand names based on the attached entitlements, therefore the actual brand name should remain unchanged.");
				expectedBrandNameAfterSubscribing = brandNameBeforeSubscribing;
			}
			if (eligibleBrandNames.size()==1) {
				expectedBrandNameAfterSubscribing = eligibleBrandNames.get(0);
				log.info("Currently there is only one eligible brand name based on the attached entitlements, therefore the actual brand name should be '"+expectedBrandNameAfterSubscribing+"'.");
				flexibleBrandedSubscriptionsFound=true;
			}

			// verify the actualBrandNameAfterSubscribing = expectedBrandNameAfterSubscribing
			String actualBrandNameAfterSubscribing = getCurrentBrandName();
			Assert.assertEquals(actualBrandNameAfterSubscribing, expectedBrandNameAfterSubscribing, "The brand name contained within the first line of the brand file '"+brandingFile+"' after subscribing to pool '"+pool.subscriptionName+"'.");
			
			// verify that the brand file was NOT altered when the expectedBrandNameAfterSubscribing = brandNameBeforeSubscribing
			if (expectedBrandNameAfterSubscribing!=null && expectedBrandNameAfterSubscribing.equals(brandNameBeforeSubscribing)) {
				String brandNameStatAfterSubscribing = getCurrentBrandNameFileStat();
				Assert.assertEquals(brandNameStatAfterSubscribing, brandNameStatBeforeSubscribing, "After attaching a new subscription, if the brand name should remain unchanged, the modification and change stats on the brand file '"+brandingFile+"' should remain unaltered.");
			}
			if (expectedBrandNameAfterSubscribing!=null && !expectedBrandNameAfterSubscribing.equals(brandNameBeforeSubscribing)) {
				String brandNameStatAfterSubscribing = getCurrentBrandNameFileStat();
				Assert.assertTrue(!brandNameStatAfterSubscribing.equals(brandNameStatBeforeSubscribing), "After attaching a new subscription, if the brand name should change, the modification/change stats on the brand file '"+brandingFile+"' should obviously change too.");
			}
			
			// verify that /usr/sbin/brandbot has updated PRETTY_NAME in /etc/os-release
			String actualPrettyNameAfterSubscribing = getCurrentPrettyName();
			if (expectedBrandNameAfterSubscribing!=null) {
				Assert.assertEquals(actualPrettyNameAfterSubscribing, expectedBrandNameAfterSubscribing, "The PRETTY_NAME in '"+osReleaseFile+"' governed by /usr/lib/systemd/system/brandbot.service should match the first line of the brand file '"+brandingFile+"' after subscribing to pool '"+pool.subscriptionName+"'.");
			}
			// TODO Not sure what PRETTY_NAME should be when expectedBrandNameAfterSubscribing==null
			// TODO Not sure what PRETTY_NAME should be when expectedBrandNameAfterSubscribing==""
		}
		
		// throw SkipException when no flexible branding was tested
		if (!flexibleBrandedSubscriptionsFound) throw new SkipException("No flexible branded subscriptions were found among the available subscriptions for testing.");
	}
	
	@Test(	description="incrementally remove attached subscriptions and verify tests for Flexible Branding",
			dependsOnGroups={"AttachSubscriptionsForFlexibleBranding_Test"},
			groups={},
			enabled=true)
	public void RemoveSubscriptionsForFlexibleBranding_Test() {
		
		// loop through all the attached subscriptions and remove them while running tests for branding
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			String brandNameBeforeUnsubscribing = getCurrentBrandName();
			String brandNameStatBeforeUnsubscribing = getCurrentBrandNameFileStat();
			log.info("Currently, the flexible brand name prior to unsubscribing from subscription '"+productSubscription.productName+"' is '"+brandNameBeforeUnsubscribing+"'.");
			clienttasks.unsubscribe(null,productSubscription.serialNumber, null, null, null);
			List<String> eligibleBrandNames = getEligibleBrandNamesFromCurrentEntitlements();
			
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
			String expectedBrandNameAfterUnsubscribing=null;
			if (eligibleBrandNames.size()>1) {
				log.warning("Currently there are multiple eligible brand names "+eligibleBrandNames+", therefore the actual brand name should remain unchanged.");
				expectedBrandNameAfterUnsubscribing = brandNameBeforeUnsubscribing;
			}
			if (eligibleBrandNames.isEmpty()) {
				log.warning("Currently there are no eligible brand names based on the attached entitlements, therefore the actual brand name should remain unchanged.");
				expectedBrandNameAfterUnsubscribing = brandNameBeforeUnsubscribing;
			}
			if (eligibleBrandNames.size()==1) {
				expectedBrandNameAfterUnsubscribing = eligibleBrandNames.get(0);
				log.info("Currently there is only one eligible brand name based on the attached entitlements, therefore the actual brand name should be '"+expectedBrandNameAfterUnsubscribing+"'.");
			}

			// verify the actualBrandNameAfterUnsubscribing = expectedBrandNameAfterUnsubscribing
			String actualBrandNameAfterUnsubscribing = getCurrentBrandName();
			Assert.assertEquals(actualBrandNameAfterUnsubscribing, expectedBrandNameAfterUnsubscribing, "The brand name contained within the first line of the brand file '"+brandingFile+"' after unsubscribing from '"+productSubscription.productName+"'.");
			
			// verify that the brand file was NOT altered when the expectedBrandNameAfterSubscribing = brandNameBeforeSubscribing
			if (expectedBrandNameAfterUnsubscribing!=null && expectedBrandNameAfterUnsubscribing.equals(brandNameBeforeUnsubscribing)) {
				String brandNameStatAfterSubscribing = getCurrentBrandNameFileStat();
				Assert.assertEquals(brandNameStatAfterSubscribing, brandNameStatBeforeUnsubscribing, "After unsubscribing, if the brand name should remain unchanged, the modification and change stats on the brand file '"+brandingFile+"' should remain unaltered.");
			}
			if (expectedBrandNameAfterUnsubscribing!=null && !expectedBrandNameAfterUnsubscribing.equals(brandNameBeforeUnsubscribing)) {
				String brandNameStatAfterUnsubscribing = getCurrentBrandNameFileStat();
				Assert.assertTrue(!brandNameStatAfterUnsubscribing.equals(brandNameStatBeforeUnsubscribing), "After unsubscribing, if the brand name should change, the modification/change stats on the brand file '"+brandingFile+"' should obviously change too.");
			}
			
			// verify that /usr/sbin/brandbot has updated PRETTY_NAME in /etc/os-release
			String actualPrettyNameAfterSubscribing = getCurrentPrettyName();
			if (expectedBrandNameAfterUnsubscribing!=null) {
				Assert.assertEquals(actualPrettyNameAfterSubscribing, expectedBrandNameAfterUnsubscribing, "The PRETTY_NAME in '"+osReleaseFile+"' governed by /usr/lib/systemd/system/brandbot.service should match the first line of the brand file '"+brandingFile+"' after unsubscribing from '"+productSubscription.productName+"'.");
			}
			// TODO Not sure what PRETTY_NAME should be when expectedBrandNameAfterSubscribing==null
			// TODO Not sure what PRETTY_NAME should be when expectedBrandNameAfterSubscribing==""
		}
	}
	
	@Test(	description="assert that brandbot only reads the first line of the branding file",
			groups={"blockedByBug-1031490"},
			enabled=true)
	public void BrandbotShouldHandleMultiLineBrandingFile_Test() {
		String actualBrandName,actualPrettyName;
		
		log.info("Testing a single line branding file...");
		RemoteFileTasks.runCommandAndAssert(client, "echo 'RHEL Branded OS (line 1)' > "+brandingFile, 0);
		actualBrandName = getCurrentBrandName();
		Assert.assertEquals(actualBrandName, "RHEL Branded OS (line 1)", "The brand name contained within the first line of the brand file '"+brandingFile+"'.");
		actualPrettyName = getCurrentPrettyName();
		Assert.assertEquals(actualPrettyName, "RHEL Branded OS (line 1)", "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"'.");

		log.info("Testing a multi-line branding file...");
		RemoteFileTasks.runCommandAndAssert(client, "echo 'RHEL SubBranded OS (line 2)' >> "+brandingFile, 0);
		actualBrandName = getCurrentBrandName();
		Assert.assertEquals(actualBrandName, "RHEL Branded OS (line 1)", "The brand name contained within the first line of the brand file '"+brandingFile+"'.");
		actualPrettyName = getCurrentPrettyName();
		Assert.assertEquals(actualPrettyName, "RHEL Branded OS (line 1)", "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' (Should not contain any new line characters).");
	}
	
	@Test(	description="assert that brandbot trims white space from the first line of the branding file",
			groups={"debugTest"},
			enabled=true)
	public void BrandbotShouldTrimWhiteSpaceFromBrandingFile_Test() {
		String actualBrandName,actualPrettyName;
		
		log.info("Testing a single line branding file with leading and trailing white space...");
		RemoteFileTasks.runCommandAndAssert(client, "echo '  RHEL Branded OS  ' > "+brandingFile, 0);
		actualBrandName = getCurrentBrandName();
		Assert.assertEquals(actualBrandName, "  RHEL Branded OS  ", "The brand name contained within the first line of the brand file '"+brandingFile+"' (should contain leading and trailing white space).");
		actualPrettyName = getCurrentPrettyName();
		Assert.assertEquals(actualPrettyName, "RHEL Branded OS", "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' (should NOT contain leading nor trailing white space).");
	}
	
	// TODO Brandbot SHOULD handle /var/lib/rhsm/branded_name not existing.
	@Test(	description="assert that brandbot trims white space from the first line of the branding file",
			groups={"debugTest"},
			enabled=false)
	public void BrandbotShouldHandleNonExistantBrandingFile_Test() {
//		String actualBrandName,actualPrettyName;
//		
//		log.info("Testing a single line branding file with leading and trailing white space...");
//		RemoteFileTasks.runCommandAndAssert(client, "echo '  RHEL Branded OS  ' > "+brandingFile, 0);
//		actualBrandName = getCurrentBrandName();
//		Assert.assertEquals(actualBrandName, "  RHEL Branded OS  ", "The brand name contained within the first line of the brand file '"+brandingFile+"' (should contain leading and trailing white space).");
//		actualPrettyName = getCurrentPrettyName();
//		Assert.assertEquals(actualPrettyName, "RHEL Branded OS", "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' (should NOT contain leading nor trailing white space).");
	}
	
	// TODO Brandbot SHOULD handle /var/lib/rhsm/branded_name being empty.
	@Test(	description="assert that brandbot trims white space from the first line of the branding file",
			groups={"debugTest"},
			enabled=false)
	public void BrandbotShouldHandleEmptyBrandingFile_Test() {
//		String actualBrandName,actualPrettyName;
//		
//		log.info("Testing a single line branding file with leading and trailing white space...");
//		RemoteFileTasks.runCommandAndAssert(client, "echo '  RHEL Branded OS  ' > "+brandingFile, 0);
//		actualBrandName = getCurrentBrandName();
//		Assert.assertEquals(actualBrandName, "  RHEL Branded OS  ", "The brand name contained within the first line of the brand file '"+brandingFile+"' (should contain leading and trailing white space).");
//		actualPrettyName = getCurrentPrettyName();
//		Assert.assertEquals(actualPrettyName, "RHEL Branded OS", "The PRETTY_NAME contained within the os-release file '"+osReleaseFile+"' (should NOT contain leading nor trailing white space).");
	}
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	
	
	// Configuration Methods ***********************************************************************
	
	
	// Protected Methods ***********************************************************************
	protected final String brandingFile = "/var/lib/rhsm/branded_name";
	protected final String osReleaseFile = "/etc/os-release";
	
	/**
	 * @return the eligible brand names based on the currently attached entitlement certs and the currently installed product certs
	 */
	List<String> getEligibleBrandNamesFromCurrentEntitlements() {
		
		// Rules:
		//  - eligible brand names come from the currently entitled productNamespaces and are identified by a brandType = "OS"
		//  - the corresponding productId must be among the currently installed product certs to be eligible
		
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();
		List<String> eligibleBrandNames = new ArrayList<String>();
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
			for (ProductNamespace productNamespace : entitlementCert.productNamespaces) {
				if (ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", productNamespace.id, currentProductCerts) != null) {
					if (productNamespace.brandType != null) {
						if (productNamespace.brandType.equals("OS")) {
							eligibleBrandNames.add(productNamespace.name);
						}
					}
				}
			}
		}
		return eligibleBrandNames;
		

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
		//	[root@jsefler-7 ~]# cat /etc/os-release
		//	NAME="Red Hat Enterprise Linux Server"
		//	VERSION="7.0 (Maipo)"
		//	ID="rhel"
		//	VERSION_ID="7.0"
		//	PRETTY_NAME="foo bar"
		//	ANSI_COLOR="0;31"
		//	CPE_NAME="cpe:/o:redhat:enterprise_linux:7.0:beta:server"
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
	
	
	
	// Data Providers ***********************************************************************
}

