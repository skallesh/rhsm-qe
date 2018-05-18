package rhsm.cli.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.ProductCert;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 *
 *	References:
 *		http://documentation-stage.bne.redhat.com/docs/en-US/Red_Hat_Enterprise_Linux/5/html/Deployment_Guide/rhn-migration.html
 *		https://engineering.redhat.com/trac/PBUPM/browser/trunk/documents/Releases/RHEL6/Variants/RHEL6-Variants.rst
 *		http://linuxczar.net/articles/rhel-installation-numbers
 *		https://docspace.corp.redhat.com/docs/DOC-71135 (PRODUCT CERTS)
 *		https://engineering.redhat.com/trac/rcm/wiki/Projects/CDNBaseline
 *
 *	RHN Migration-Data Mappings
 *		This document describes the process and policy for when these updates are to be made to the migration data mappings.
 *		https://mojo.redhat.com/docs/DOC-984684
 *
 *	Red Hat Product Certificates
 *		https://mojo.redhat.com/docs/DOC-103535
 *
 *  Note: Rich Jerrido says...
 *  	We depend on migration-data heavily for satellite 6's bootstrap script.
 *  	bootstrap.py is the primary means to migrate systems from {RHN,RHSM,SAM,Sat5} to Satellite 6.
 *      We explicitly call rhn-migrate-classic-to-rhsm when needed to deploy product certs. (And for the platforms where rhn-migrate doesn't work the way we want, RHEL5, we explicitly fetch product certs from /usr/share/RHSM) 
 *  	https://github.com/Katello/katello-client-bootstrap
 *  	https://access.redhat.com/articles/2280691
 *  
 *	// OLD LOCATION
 *	git clone git://git.app.eng.bos.redhat.com/rcm/rhn-definitions.git
 *  http://git.app.eng.bos.redhat.com/?p=rcm/rhn-definitions.git;a=tree
 *  
 *  git clone git://git.app.eng.bos.redhat.com/rcm/rcm-metadata.git
 *  http://git.app.eng.bos.redhat.com/git/rcm/rcm-metadata.git/tree/
 *  
 *  product 150 is at
 *  http://git.app.eng.bos.redhat.com/?p=rcm/rhn-definitions.git;a=tree;f=product_ids/rhev-3.0;hb=HEAD
 *
 *  for F in `rpm -ql subscription-manager-migration-data | grep .pem`; do echo ""; rct cat-cert $F | grep -i "Version"; done;
 */
@Test(groups={"MigrationDataTests"})
public class MigrationDataTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20097", "RHEL7-51104"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1 Tier2 Tier3")
	@Test(	description="Verify that the channel-cert-mapping.txt exists",
			groups={"Tier1Tests","Tier2Tests","Tier3Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testChannelCertMappingFileExists() {
		Assert.assertTrue(RemoteFileTasks.testExists(client, channelCertMappingFilename),"The expected channel cert mapping file '"+channelCertMappingFilename+"' exists.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20100", "RHEL7-51107"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1 Tier2 Tier3")
	@Test(	description="Verify that the channel-cert-mapping.txt contains a unique map of channels to product certs",
			groups={"Tier1Tests","Tier2Tests","Tier3Tests"},
			dependsOnMethods={"testChannelCertMappingFileExists"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testChannelCertMapping() throws FileNotFoundException, IOException {
		Assert.assertTrue(RemoteFileTasks.testExists(client, channelCertMappingFilename),"The expected channel cert mapping file '"+channelCertMappingFilename+"' exists.");
		
		// Read the channelCertMappingFilename as if they were properties (Warning! this will mask non-unique mappings)
		// [root@jsefler-onprem-5client ~]# cat /usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt
		// rhn-tools-rhel-x86_64-server-5-beta: none
		// rhn-tools-rhel-x86_64-server-5: Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem
		// rhn-tools-rhel-x86_64-client-5-beta: none
		// rhn-tools-rhel-x86_64-client-5: Client-Client-x86_64-efe91c1c-78d7-4d19-b2fb-3c88cfc2da35-68.pem
		SSHCommandResult result = client.runCommandAndWait("cat "+channelCertMappingFilename);
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(result.getStdout().getBytes("UTF-8")));
		for (Object key: p.keySet()){
			// load the channelsToProductCertFilesMap
			channelsToProductCertFilenamesMap.put((String)key, p.getProperty((String)(key)));
			// load the mappedProductCertFiles
			if (!channelsToProductCertFilenamesMap.get(key).equalsIgnoreCase("none"))
				mappedProductCertFilenames.add(channelsToProductCertFilenamesMap.get(key));
		}
		
		// Read the channelCertMappingFilename line by line asserting unique mappings
		boolean uniqueChannelsToProductCertFilenamesMap = true;
		for (String line: result.getStdout().trim().split("\\n")){
			if (line.trim().equals("")) continue; // skip blank lines
			if (line.trim().startsWith("#")) continue; // skip comments
			String channel = line.split(":")[0].trim();
			String productCertFilename = line.split(":")[1].trim();
			if (channelsToProductCertFilenamesMap.containsKey(channel)) {
				if (!channelsToProductCertFilenamesMap.get(channel).equals(productCertFilename)) {
					log.warning("RHN Channel '"+channel+"' is already mapped to productFilename '"+productCertFilename+"' while parsing "+channelCertMappingFilename+" line: "+line);
					uniqueChannelsToProductCertFilenamesMap = false;
				}
			} else {
				Assert.fail("Having trouble parsing the following channel:product map from "+channelCertMappingFilename+": "+line);
			}
		}
		Assert.assertTrue(uniqueChannelsToProductCertFilenamesMap, "Each channel in "+channelCertMappingFilename+" maps to a unique product cert filename. (See above warnings for offenders.)");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20101", "RHEL7-51108"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Verify RHEL4 channel mappings exist in channel-cert-mapping.txt",
			groups={"Tier3Tests","blockedByBug-1009932","blockedByBug-1025338","blockedByBug-1080072","blockedByBug-1100872"},
			dependsOnMethods={"testChannelCertMappingFileExists"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRHEL4ChannelMappings() {
		
		// hand assemble a list of expected RHEL4 channels
		// [root@jsefler-6 ~]# egrep 'rhel-.*-4(:|-.*:|\.[[:digit:]]\.z:)' /usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt | egrep --invert-match  '(-ost-4|-4-els|-4-hwcert)'
		// rhel-i386-as-4: AS-AS-i386-002dbe5bbca3-69.pem
		List<String> expectedRhel4Channels = Arrays.asList(new String[]{
				"rhel-i386-as-4",
				//"rhel-i386-as-4-els",		// Name: Red Hat Enterprise Linux Server - Extended Life Cycle Support
				//"rhel-i386-as-4-hwcert",	// Name: Red Hat Hardware Certification Test Suite
				"rhel-i386-as-4.5.z",
				"rhel-i386-as-4.6.z",
				"rhel-i386-as-4.7.z",
				"rhel-i386-as-4.8.z",
				"rhel-i386-es-4",
				"rhel-i386-es-4.5.z",
				"rhel-i386-es-4.6.z",
				"rhel-i386-es-4.7.z",
				"rhel-i386-es-4.8.z",
				"rhel-i386-ws-4",
				"rhel-i386-ws-4-beta",		// added by subscription-manager-migration-data-2.0.10-1.el5
				"rhel-ia64-as-4",
				//"rhel-ia64-as-4-hwcert",
				"rhel-ia64-as-4.5.z",
				"rhel-ia64-as-4.6.z",
				"rhel-ia64-as-4.7.z",
				"rhel-ia64-as-4.8.z",
				"rhel-ia64-es-4",
				"rhel-ia64-ws-4-beta",		// added by subscription-manager-migration-data-2.0.10-1.el5
				"rhel-ia64-es-4.5.z",
				"rhel-ia64-es-4.6.z",
				"rhel-ia64-es-4.7.z",
				"rhel-ia64-es-4.8.z",
				"rhel-ia64-ws-4",
				"rhel-ppc-as-4",
				"rhel-ppc-as-4.5.z",
				"rhel-ppc-as-4.6.z",
				"rhel-ppc-as-4.7.z",
				"rhel-ppc-as-4.8.z",
				"rhel-s390-as-4",
				"rhel-s390-as-4.5.z",
				"rhel-s390-as-4.6.z",
				"rhel-s390-as-4.7.z",
				"rhel-s390-as-4.8.z",
				"rhel-s390x-as-4",
				"rhel-s390x-as-4.5.z",
				"rhel-s390x-as-4.6.z",
				"rhel-s390x-as-4.7.z",
				"rhel-s390x-as-4.8.z",
				"rhel-x86_64-as-4",
				//"rhel-x86_64-as-4-els",
				//"rhel-x86_64-as-4-hwcert",
				"rhel-x86_64-as-4.5.z",
				"rhel-x86_64-as-4.6.z",
				"rhel-x86_64-as-4.7.z",
				"rhel-x86_64-as-4.8.z",
				"rhel-x86_64-es-4",
				"rhel-x86_64-es-4.5.z",
				"rhel-x86_64-es-4.6.z",
				"rhel-x86_64-es-4.7.z",
				"rhel-x86_64-es-4.8.z",
				//"rhel-x86_64-server-6-ost-4",		// Name: Red Hat OpenStack
				//"rhel-x86_64-server-6-ost-4-cts",
				//"rhel-x86_64-server-6-ost-4-cts-debuginfo",
				//"rhel-x86_64-server-6-ost-4-debuginfo",
				"rhel-x86_64-ws-4",
				"rhel-x86_64-ws-4-beta"		// added by subscription-manager-migration-data-2.0.10-1.el5
		});
		
		// use a regex and grep to detect actual RHEL4 channel mappings
		List<String> actualRhel4Channels = new ArrayList<String>();
		SSHCommandResult result = client.runCommandAndWait("egrep 'rhel-.*-4(:|-.*:|\\.[[:digit:]]\\.z:)' "+channelCertMappingFilename+" | egrep --invert-match '(-ost-4|-4-els|-4-hwcert)'");
		for (String line: result.getStdout().trim().split("\\n")){
			if (line.trim().equals("")) continue; // skip blank lines
			if (line.trim().startsWith("#")) continue; // skip comments
			String channel = line.split(":")[0].trim();
			String productCertFilename = line.split(":")[1].trim();
			actualRhel4Channels.add(channel);
		}
		
		boolean allExpectedRhel4ChannelsAreMapped = true;
		for (String channel : expectedRhel4Channels) {
			if (actualRhel4Channels.contains(channel)) {
				Assert.assertTrue(actualRhel4Channels.contains(channel), "Expected RHEL4 channel '"+channel+"' was found in channel cert mapping file '"+channelCertMappingFilename+"'.");
			} else {
				log.warning("Expected RHEL4 channel '"+channel+"' was not found in channel cert mapping file '"+channelCertMappingFilename+"'.");
				allExpectedRhel4ChannelsAreMapped = false;
			}
		}
		
		boolean allActualRhel4ChannelsMappedAreExpected = true;
		for (String channel : actualRhel4Channels) {
			if (!expectedRhel4Channels.contains(channel)) {
				log.warning("Actual RHEL4 channel '"+channel+"' in channel cert mapping file '"+channelCertMappingFilename+"' that was not expected.  This automated test may need an update.");
				allActualRhel4ChannelsMappedAreExpected = false;
			}
		}
		
		Assert.assertTrue(allExpectedRhel4ChannelsAreMapped, "All expected RHEL4 channels are mapped in '"+channelCertMappingFilename+"'. (See above warnings for offenders.)");
		Assert.assertTrue(allActualRhel4ChannelsMappedAreExpected, "All actual RHEL4 channels mapped in '"+channelCertMappingFilename+"' are expected. (See above warnings for offenders.)");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21883", "RHEL7-51738"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Verify RHEL6 channel mappings for AUS channels exist in channel-cert-mapping.txt",
			groups={"Tier3Tests","blockedByBug-825089"},
			dependsOnMethods={"testChannelCertMappingFileExists"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRHEL6AUSChannelMappings() {
		
		// What is AUS? Advanced Mission Critical Update Support
		// For RHEL 6, AUS provides access to a 6-year “Long Life” maintenance stream for specific minor releases (4 years longer than EUS)
		// For RHEL 6, EUS provides a 24-month maintenance stream for every Production 1 phase minor release (GA+24 months)
		// https://mojo.redhat.com/docs/DOC-1027471
		// AUS is EUS for customers who slower evolution
		
		// hand assemble a list of expected RHEL6 aus channels (WILL NEED TO BE UPDATED AS NEW AUS CHANNELS ARE SUPPORTED)
		//	[root@jsefler-os6 ~]# grep ".aus" /usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt
		//	rhel-x86_64-server-6.2.aus: AUS-Server-x86_64-570a9cca61c9-251.pem
		//	[root@jsefler-os6 ~]# grep ".aus" /usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt | cut -d: -f 1 | xargs -i[] echo \"[]\",
		//	"rhel-x86_64-server-6.2.aus",

		// [root@jsefler-6 ~]# egrep 'rhel-.*-4(:|-.*:|\.[[:digit:]]\.z:)' /usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt | egrep --invert-match  '(-ost-4|-4-els|-4-hwcert)'
		// rhel-i386-as-4: AS-AS-i386-002dbe5bbca3-69.pem
		List<String> expectedRhel6AusChannels = Arrays.asList(new String[]{
				"rhel-x86_64-server-6.2.aus",
				"rhel-x86_64-server-6.2.aus-debuginfo",
				"rhel-x86_64-server-6.2.aus-thirdparty-oracle-java",	// TODO
				"rhel-x86_64-server-6.4.aus",
				"rhel-x86_64-server-6.4.aus-debuginfo",
				"rhel-x86_64-server-6.4.aus-thirdparty-oracle-java",	// TODO
				"rhel-x86_64-server-6.5.aus",
				"rhel-x86_64-server-6.5.aus-debuginfo",
				"rhel-x86_64-server-6.5.aus-thirdparty-oracle-java",	// TODO
				"rhel-x86_64-server-6.6.aus",
				"rhel-x86_64-server-6.6.aus-debuginfo",
				"rhel-x86_64-server-6.6.aus-thirdparty-oracle-java",
				"rhel-x86_64-server-ha-6.2.aus",
				"rhel-x86_64-server-ha-6.2.aus-debuginfo",
				"rhel-x86_64-server-ha-6.4.aus",
				"rhel-x86_64-server-ha-6.4.aus-debuginfo",
				"rhel-x86_64-server-ha-6.5.aus",
				"rhel-x86_64-server-ha-6.5.aus-debuginfo",
				"rhel-x86_64-server-lb-6.2.aus",
				"rhel-x86_64-server-lb-6.2.aus-debuginfo",
				"rhel-x86_64-server-lb-6.4.aus",
				"rhel-x86_64-server-lb-6.4.aus-debuginfo",
				"rhel-x86_64-server-lb-6.5.aus",
				"rhel-x86_64-server-lb-6.5.aus-debuginfo",
				"rhel-x86_64-server-optional-6.2.aus",
				"rhel-x86_64-server-optional-6.2.aus-debuginfo",
				"rhel-x86_64-server-optional-6.4.aus",
				"rhel-x86_64-server-optional-6.4.aus-debuginfo",
				"rhel-x86_64-server-optional-6.5.aus",
				"rhel-x86_64-server-optional-6.5.aus-debuginfo",
				"rhel-x86_64-server-optional-6.6.aus",
				"rhel-x86_64-server-optional-6.6.aus-debuginfo",
				"rhel-x86_64-server-rs-6.2.aus",
				"rhel-x86_64-server-rs-6.2.aus-debuginfo",
				"rhel-x86_64-server-rs-6.4.aus",
				"rhel-x86_64-server-rs-6.4.aus-debuginfo",
				"rhel-x86_64-server-rs-6.5.aus",
				"rhel-x86_64-server-rs-6.5.aus-debuginfo",
				"rhel-x86_64-server-sfs-6.2.aus",
				"rhel-x86_64-server-sfs-6.2.aus-debuginfo",
				"rhel-x86_64-server-sfs-6.4.aus",
				"rhel-x86_64-server-sfs-6.4.aus-debuginfo",
				"rhel-x86_64-server-sfs-6.5.aus",
				"rhel-x86_64-server-sfs-6.5.aus-debuginfo",
// TODO: Bug 1418467 - NEEDINFO on rhel-x86_64-server-sjis-6.*.aus-debuginfo channel mappings to product certs before including...
// DONE: Bug 1418467 was CLOSED WONTFIX.  Inclusion/verification of these three channels is benign. 
					"rhel-x86_64-server-sjis-6.2.aus-debuginfo",
					"rhel-x86_64-server-sjis-6.4.aus-debuginfo",
					"rhel-x86_64-server-sjis-6.5.aus-debuginfo",
				"rhel-x86_64-server-supplementary-6.2.aus",
				"rhel-x86_64-server-supplementary-6.2.aus-debuginfo",
				"rhel-x86_64-server-supplementary-6.4.aus",
				"rhel-x86_64-server-supplementary-6.4.aus-debuginfo",
				"rhel-x86_64-server-supplementary-6.5.aus",
				"rhel-x86_64-server-supplementary-6.5.aus-debuginfo",
				"rhel-x86_64-server-supplementary-6.6.aus",
				"rhel-x86_64-server-supplementary-6.6.aus-debuginfo",
				"rhn-tools-rhel-x86_64-server-6.2.aus",
				"rhn-tools-rhel-x86_64-server-6.2.aus-debuginfo",
				"rhn-tools-rhel-x86_64-server-6.4.aus",
				"rhn-tools-rhel-x86_64-server-6.4.aus-debuginfo",
				"rhn-tools-rhel-x86_64-server-6.5.aus",
				"rhn-tools-rhel-x86_64-server-6.5.aus-debuginfo",
				"rhn-tools-rhel-x86_64-server-6.6.aus",
				"rhn-tools-rhel-x86_64-server-6.6.aus-debuginfo"
		});
		
		// use a regex and grep to detect actual RHEL6 aus channel mappings
		List<String> actualRhel6AusChannels = new ArrayList<String>();
		SSHCommandResult result = client.runCommandAndWait("grep '.aus' "+channelCertMappingFilename);
		for (String line: result.getStdout().trim().split("\\n")){
			if (line.trim().equals("")) continue; // skip blank lines
			if (line.trim().startsWith("#")) continue; // skip comments
			String channel = line.split(":")[0].trim();
			String productCertFilename = line.split(":")[1].trim();
			
			// exclude RHEL7 aus channels // examples: rhel-x86_64-server-7.2.aus rhel-x86_64-server-optional-7.2.aus
			if (doesStringContainMatches(channel, "-7\\.\\d\\.aus")) {
				log.info("Ignoring RHEL7 aus channel '"+channel+"'");
				continue;
			}
			
			actualRhel6AusChannels.add(channel);
		}
		
		boolean allExpectedRhel6AusChannelsAreMapped = true;
		for (String channel : expectedRhel6AusChannels) {
			if (actualRhel6AusChannels.contains(channel)) {
				Assert.assertTrue(actualRhel6AusChannels.contains(channel), "Expected RHEL6 AUS channel '"+channel+"' was found in channel cert mapping file '"+channelCertMappingFilename+"'.");
			} else {
				log.warning("Expected RHEL6 AUS channel '"+channel+"' was not found in channel cert mapping file '"+channelCertMappingFilename+"'.");
				allExpectedRhel6AusChannelsAreMapped = false;
			}
		}
		
		boolean allActualRhel6AusChannelsMappedAreExpected = true;
		for (String channel : actualRhel6AusChannels) {
			if (!expectedRhel6AusChannels.contains(channel)) {
				
				// TEMPORARY WORKAROUND
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1418467";	// Bug 1418467 - NEEDINFO on rhel-x86_64-server-sjis-6.*.aus-debuginfo channel mappings to product certs
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen && channel.matches("rhel-x86_64-server-sjis-6\\..\\.aus-debuginfo")) {
					log.warning("Skipping assertion that Actual RHEL6 AUS channel '"+channel+"' in channel cert mapping file '"+channelCertMappingFilename+"' was not expected while bug "+bugId+" is open.");
					continue;
				}
				// END OF WORKAROUND
				
				log.warning("Actual RHEL6 AUS channel '"+channel+"' in channel cert mapping file '"+channelCertMappingFilename+"' was not expected.  This automated test may need an update.");
				allActualRhel6AusChannelsMappedAreExpected = false;
			}
		}
		
		Assert.assertTrue(allExpectedRhel6AusChannelsAreMapped, "All expected RHEL6 AUS channels are mapped in '"+channelCertMappingFilename+"'. (See above warnings for offenders.)");
		Assert.assertTrue(allActualRhel6AusChannelsMappedAreExpected, "All actual RHEL6 AUS channels mapped in '"+channelCertMappingFilename+"' are expected. (See above warnings for offenders.)");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20103", "RHEL7-51110"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that all product cert files mapped in channel-cert-mapping.txt exist",
			groups={"Tier1Tests","blockedByBug-771615"},
			dependsOnMethods={"testChannelCertMapping"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAllMappedProductCertFilesExists() {

		boolean allMappedProductCertFilesExist = true;
		for (String mappedProductCertFilename : mappedProductCertFilenames) {
			String mappedProductCertFile = baseProductsDir+"/"+mappedProductCertFilename;
			if (RemoteFileTasks.testExists(client, mappedProductCertFile)) {
				log.info("Mapped productCert file '"+mappedProductCertFile+"' exists.");		
			} else {
				log.warning("Mapped productCert file '"+mappedProductCertFile+"' does NOT exist.");
				allMappedProductCertFilesExist = false;
			}
		}
		Assert.assertTrue(allMappedProductCertFilesExist,"All of the productCert files mapped in '"+channelCertMappingFilename+"' exist.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20102", "RHEL7-51109"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that all existing product cert files are mapped in channel-cert-mapping.txt",
			groups={"Tier1Tests","blockedByBug-799103","blockedByBug-849274","blockedByBug-909436","blockedByBug-1025338"},
			dependsOnMethods={"testChannelCertMapping"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAllExistingProductCertFilesAreMapped() {
		
		// get a list of all the existing product cert files
		SSHCommandResult result = client.runCommandAndWait("ls "+baseProductsDir+"/*.pem");
		Assert.assertEquals(result.getExitCode(), new Integer(0), "Exit code from a list of all migration data product certs.");
		List<String> existingProductCertFiles = Arrays.asList(result.getStdout().split("\\n"));
		boolean allExitingProductCertFilesAreMapped = true;
		for (String existingProductCertFile : existingProductCertFiles) {
			if (mappedProductCertFilenames.contains(new File(existingProductCertFile).getName())) {
				log.info("Existing productCert file '"+existingProductCertFile+"' is mapped in '"+channelCertMappingFilename+"'.");
			} else {
				log.warning("Existing productCert file '"+existingProductCertFile+"' is NOT mapped in '"+channelCertMappingFilename+"'.");
				
				// TEMPORARY WORKAROUND FOR BUG
				/* Notes: http://entitlement.etherpad.corp.redhat.com/Entitlement02MAY12 
			    /product_ids/rhel-6.3/ComputeNode-ScalableFileSystem-x86_64-21b36280d242-175.pem  is not mapped to any RHN Channels in /cdn/product-baseline.json  (SEEMS  WRONG)
			    (dgregor) channel won't exist until 6.3 GA.  suggest we pick this up in 6.4
			    (jsefler) TODO update automated test with pre-6.3GA work-around
			    /product_ids/rhel-6.3/Server-HPN-ppc64-fff6dded9725-173.pem  is not mapped to  any RHN Channels in /cdn/product-baseline.json   (SEEMS WRONG)
			    (dgregor) channel won't exist until 6.3 GA.  suggest we pick this up in 6.4
			    (jsefler) TODO update automated test with pre-6.3GA work-around
			    */
				if (existingProductCertFile.endsWith("-173.pem") && clienttasks.redhatReleaseXY.equals("6.3")) {
					log.warning("Ignoring that existing productCert file '"+existingProductCertFile+"' is NOT mapped in '"+channelCertMappingFilename+"' until release 6.4 as recommended by dgregor.");
				} else
				if (existingProductCertFile.endsWith("-175.pem") && clienttasks.redhatReleaseXY.equals("6.3")) {
					log.warning("Ignoring that existing productCert file '"+existingProductCertFile+"' is NOT mapped in '"+channelCertMappingFilename+"' until release 6.4 as recommended by dgregor.");
				} else
				// END OF WORKAROUND
				allExitingProductCertFilesAreMapped = false;
				
			}
		}
		Assert.assertTrue(allExitingProductCertFilesAreMapped,"All of the existing productCert files in directory '"+baseProductsDir+"' are mapped to a channel in '"+channelCertMappingFilename+"'.");
	}
	
	@Deprecated
	@Test(	description="Verify that the migration product certs support this system's RHEL release version",
			groups={"Tier1Tests","blockedByBug-782208"},
			dependsOnMethods={"testChannelCertMapping"},
			enabled=false)	// 9/12/2013 RHEL65: disabled in favor of new VerifyMigrationProductCertsSupportThisSystemsRhelVersion_Test; this old test was based on the generation of subscription-manager-migration-data from product-baseline.json
	@ImplementsNitrateTest(caseId=130940)
	public void testMigrationProductCertsSupportThisSystemsRhelVersion_DEPRECATED() {
		
		// process all the migration product cert files into ProductCerts and assert their version
		boolean verifiedVersionOfAllMigrationProductCertFiles = true;
		for (ProductCert productCert : clienttasks.getProductCerts(baseProductsDir)) {
			if (!productCert.productNamespace.providedTags.toLowerCase().contains("rhel")) {
				log.warning("Migration productCert '"+productCert+"' does not provide RHEL tags.  Skipping assertion that its version matches this system's RHEL version.");
				continue;
			}
			if (productCert.productNamespace.version.equals(clienttasks.redhatReleaseXY)) {
				Assert.assertTrue(true,"Migration productCert '"+productCert+"' supports this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");

			} else {
				log.warning("Migration productCert '"+productCert+"' does NOT support this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
				verifiedVersionOfAllMigrationProductCertFiles = false;
			}
		}
		Assert.assertTrue(verifiedVersionOfAllMigrationProductCertFiles,"All of the migration productCerts in directory '"+baseProductsDir+"' support this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
	}

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20106", "RHEL7-51111"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that the migration product certs support this system's RHEL release version",
			groups={"Tier1Tests","blockedByBug-782208","blockedByBug-1006060","blockedByBug-1025338","blockedByBug-1080072","blockedByBug-1110863","blockedByBug-1148110","blockedByBug-1197864","blockedByBug-1300766","blockedByBug-1241221","blockedByBug-1328579","blockedByBug-1393573","blockedByBug-1436441","blockedByBug-1510200","blockedByBug-1555913"},
			dependsOnMethods={"testChannelCertMapping"},
			enabled=true)
	@ImplementsNitrateTest(caseId=130940)
	public void testMigrationProductCertsSupportThisSystemsRhelVersion() {
		
		// process all the migration product cert files into ProductCerts and assert their version
		boolean verifiedVersionOfAllMigrationProductCertFiles = false;
		int numberOfMigrationProductCertsSupportingThisRelease = 0;
		for (ProductCert productCert : clienttasks.getProductCerts(baseProductsDir)) {
			if (productCert.productNamespace.providedTags==null || !productCert.productNamespace.providedTags.toLowerCase().contains("rhel")) {
				log.info("Migration productCert '"+productCert.file+"' providesTags '"+productCert.productNamespace.providedTags+"' which are NOT RHEL tags.  Skipping assertion that its version matches this system's RHEL version.");
				continue;
			}
			if (productCert.productNamespace.version.equals(clienttasks.redhatReleaseXY)) {
				Assert.assertTrue(true,"Migration productCert '"+productCert.file+"' supports this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
				numberOfMigrationProductCertsSupportingThisRelease++;
			} else {
				log.warning("Migration productCert '"+productCert.file+"' providesTags '"+productCert.productNamespace.providedTags+"' version '"+productCert.productNamespace.version+"' does NOT support this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
			}
		}
		Assert.assertTrue(numberOfMigrationProductCertsSupportingThisRelease>0,"At least one 'actual="+numberOfMigrationProductCertsSupportingThisRelease+"' migration productCerts in directory '"+baseProductsDir+"' support this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
	}
	
	
	@Deprecated
	@Test(	description="Verify that the migration product certs match those from rhn definitions",
			groups={"Tier1Tests","blockedByBug-799152","blockedByBug-814360","blockedByBug-861420","blockedByBug-861470","blockedByBug-872959","blockedByBug-875760","blockedByBug-875802"},
			enabled=false)	// 9/9/2013 RHEL65: disabled in favor of new VerifyMigrationProductCertsMatchThoseFromRhnDefinitions_Test
	//@ImplementsNitrateTest(caseId=)
	public void testMigrationProductCertsMatchThoseFromRhnDefinitions_DEPRECATED() {
		
		// process all the migration product cert files into ProductCerts and assert they match those from the RHN Definitions

		// get all of the rhnDefnition product certs
		List<ProductCert> rhnDefnitionProductCerts = new ArrayList<ProductCert>();
		for (String rhnDefinitionsProductCertsDir : sm_rhnDefinitionsProductCertsDirs) {
			String tmpRhnDefinitionsProductCertsDir = clienttasks.rhnDefinitionsDir+rhnDefinitionsProductCertsDir;
			Assert.assertTrue(RemoteFileTasks.testExists(client, tmpRhnDefinitionsProductCertsDir),"The rhn definitions product certs dir '"+rhnDefinitionsProductCertsDir+"' has been locally cloned to '"+tmpRhnDefinitionsProductCertsDir+"'.");
			rhnDefnitionProductCerts.addAll(clienttasks.getProductCerts(tmpRhnDefinitionsProductCertsDir));
		}
		/* ALTERNATIVE WAY OF GETTING ALL rhnDefnition PRODUCT CERTS FROM ALL DIRECTORIES
		SSHCommandResult result = client.runCommandAndWait("find "+clienttasks.rhnDefinitionsDir+"/product_ids/ -name '*.pem'");
		String[] rhnDefnitionProductCertPaths = result.getStdout().trim().split("\\n");
		if (rhnDefnitionProductCertPaths.length==1 && rhnDefnitionProductCertPaths[0].equals("")) rhnDefnitionProductCertPaths = new String[]{};
		for (String rhnDefnitionProductCertPath : rhnDefnitionProductCertPaths) {
			rhnDefnitionProductCerts.add(clienttasks.getProductCertFromProductCertFile(new File(rhnDefnitionProductCertPath)));
		}
		*/
		
		// get the local migration product certs available for install
		List<ProductCert> migrationProductCerts = clienttasks.getProductCerts(baseProductsDir);

		// test that these local migration product certs came from the current rhnDefinitions structure
		boolean verifiedMatchForAllMigrationProductCertFiles = true;
		for (ProductCert migrationProductCert : migrationProductCerts) {
			if (rhnDefnitionProductCerts.contains(migrationProductCert)) {
				Assert.assertTrue(true, "Migration product cert '"+migrationProductCert.file+"' was found among the product certs declared for this release from ["+sm_rhnDefinitionsGitRepository+"] "+sm_rhnDefinitionsProductCertsDirs);
			} else {
				log.warning("Migration product cert '"+migrationProductCert.file+"' was NOT found among the product certs declared for this release from ["+sm_rhnDefinitionsGitRepository+"] "+sm_rhnDefinitionsProductCertsDirs+".  It may have been re-generated by release engineering.");
				verifiedMatchForAllMigrationProductCertFiles = false;
			}
		}
		
		// now assert that all of product certs from the current rhnDefinitions structure are locally available for install
		for (ProductCert rhnDefinitionProductCert : rhnDefnitionProductCerts) {
			if (migrationProductCerts.contains(rhnDefinitionProductCert)) {
				Assert.assertTrue(true, "CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was found among the local migration product certs available for installation.");
			} else {
				
				// determine if the rhnDefinitionProductCert is not mapped to any RHEL [5|6] RHN Channels defined in the product baseline file
				List<String> rhnChannels = cdnProductBaselineProductIdMap.get(rhnDefinitionProductCert.productId);
				if (rhnChannels==null) {
					log.warning("CDN Product Baseline has an empty list of RHN Channels for Product ID '"+rhnDefinitionProductCert.productId+"' Name '"+rhnDefinitionProductCert.productName+"'.  This could be a rel-eng defect.");
					rhnChannels = new ArrayList<String>();
				}
				Set<String> rhnChannelsFilteredForRhelRelease = new HashSet<String>();
				for (String rhnChannel : rhnChannels) {
					// filter out all RHN Channels not associated with this release  (e.g., assume that an rhn channel containing "-5-" or ends in "-5" is only applicable to rhel5 
					if (!(rhnChannel.contains("-"+clienttasks.redhatReleaseX+"-") || rhnChannel.endsWith("-"+clienttasks.redhatReleaseX))) continue;
					rhnChannelsFilteredForRhelRelease.add(rhnChannel);
				}
				if (rhnChannelsFilteredForRhelRelease.isEmpty()) {
					log.info("CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was NOT found among the current migration product certs.  No RHEL '"+clienttasks.redhatReleaseX+"' RHN Channels in '"+sm_rhnDefinitionsProductBaselineFile+"' map to Product ID '"+rhnDefinitionProductCert.productId+"' Name '"+rhnDefinitionProductCert.productName+"'.");	
				} else {
					log.warning("CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was NOT found among the current migration product certs.  It is probably a new product cert generated by release engineering and therefore subscription-manager-migration-data needs a regeneration.");
					verifiedMatchForAllMigrationProductCertFiles = false;
				}
			}
		}
		
		Assert.assertTrue(verifiedMatchForAllMigrationProductCertFiles,"All of the migration productCerts in directory '"+baseProductsDir+"' match the current ["+sm_rhnDefinitionsGitRepository+"] product certs for this RHEL release '"+clienttasks.redhatReleaseXY+"' ");
	}

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20098", "RHEL7-51105"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that the migration product certs match those from rhn definitions",
			groups={"Tier1Tests","blockedByBug-799152","blockedByBug-814360","blockedByBug-861420","blockedByBug-861470","blockedByBug-872959","blockedByBug-875760","blockedByBug-875802","blockedByBug-1305695","blockedByBug-1555913"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testMigrationProductCertsMatchThoseFromRhnDefinitions() {
		
		// assemble a list of rhnDefinitionsProductCertsDirs that we care about under [rcm/rcm-metadata.git] / product_ids /
		// Note: we care about all of the productCertsDirs
		SSHCommandResult result = client.runCommandAndWait("find "+clienttasks.rhnDefinitionsDir+"/product_ids -maxdepth 1 -type d");
		List<String> rhnDefinitionsProductCertsDirs = new ArrayList<String>();
		for (String productIdsDir : result.getStdout().split("\\n")) {
			if (!productIdsDir.equals(clienttasks.rhnDefinitionsDir+"/product_ids")) {
				// put logic here to exclude specific directories
				
				rhnDefinitionsProductCertsDirs.add(productIdsDir);
			}
		}
		Assert.assertTrue(!rhnDefinitionsProductCertsDirs.isEmpty(),"The "+clienttasks.rhnDefinitionsDir+"/product_ids is not empty.");
		
		
		// process all the migration product cert files into ProductCerts and assert they match those from the RHN Definitions

		// get all of the rhnDefnition product certs
		List<ProductCert> rhnDefnitionProductCerts = new ArrayList<ProductCert>();
		for (String rhnDefinitionsProductCertsDir : /*sm_*/rhnDefinitionsProductCertsDirs) {
			String tmpRhnDefinitionsProductCertsDir = /*clienttasks.rhnDefinitionsDir+*/rhnDefinitionsProductCertsDir;
			Assert.assertTrue(RemoteFileTasks.testExists(client, tmpRhnDefinitionsProductCertsDir),"The rhn definitions product certs dir '"+rhnDefinitionsProductCertsDir+"' has been locally cloned to '"+tmpRhnDefinitionsProductCertsDir+"'.");
			rhnDefnitionProductCerts.addAll(clienttasks.getProductCerts(tmpRhnDefinitionsProductCertsDir));
		}
		/* ALTERNATIVE WAY OF GETTING ALL rhnDefnition PRODUCT CERTS FROM ALL DIRECTORIES
		SSHCommandResult result = client.runCommandAndWait("find "+clienttasks.rhnDefinitionsDir+"/product_ids/ -name '*.pem'");
		String[] rhnDefnitionProductCertPaths = result.getStdout().trim().split("\\n");
		if (rhnDefnitionProductCertPaths.length==1 && rhnDefnitionProductCertPaths[0].equals("")) rhnDefnitionProductCertPaths = new String[]{};
		for (String rhnDefnitionProductCertPath : rhnDefnitionProductCertPaths) {
			rhnDefnitionProductCerts.add(clienttasks.getProductCertFromProductCertFile(new File(rhnDefnitionProductCertPath)));
		}
		*/
		
		// get the local migration product certs available for install
		List<ProductCert> migrationProductCerts = clienttasks.getProductCerts(baseProductsDir);

		// test that these local migration product certs came from the current rhnDefinitions structure
		boolean verifiedMatchForAllMigrationProductCertFiles = true;
		for (ProductCert migrationProductCert : migrationProductCerts) {
			if (rhnDefnitionProductCerts.contains(migrationProductCert)) {
//LOGGING HOG	Assert.assertTrue(true, "Migration product cert '"+migrationProductCert.file+"' was found among the product certs declared for this release from ["+sm_rhnDefinitionsGitRepository+"] "+/*sm_*/rhnDefinitionsProductCertsDirs);
				Assert.assertTrue(true, "Migration product cert '"+migrationProductCert.file+"' was found among the product certs declared for this release from the product_ids directory under ["+sm_rhnDefinitionsGitRepository+"]");
			} else {
				log.warning("Migration product cert '"+migrationProductCert.file+"' was NOT found among the product certs declared for this release from ["+sm_rhnDefinitionsGitRepository+"] "+/*sm_*/rhnDefinitionsProductCertsDirs+".  It may have been re-generated by release engineering.");
				log.info ("Searching the current product certs defined by release engineering for a newly generated instance of this migration product cert...");
				boolean foundNewerVersionOfMigrationProductCert = false;
				for (ProductCert rhnDefnitionProductCert : rhnDefnitionProductCerts) {
					Set<String> rhnDefnitionProductCertProvidedTags = new HashSet<String>();
					Set<String> migrationProductCertProvidedTags = new HashSet<String>();
					if (rhnDefnitionProductCert.productNamespace.providedTags!=null) rhnDefnitionProductCertProvidedTags.addAll(Arrays.asList(rhnDefnitionProductCert.productNamespace.providedTags.split("\\s*,\\s*")));	// tags can be defined as a comma separated string
					if (migrationProductCert.productNamespace.providedTags!=null) migrationProductCertProvidedTags.addAll(Arrays.asList(migrationProductCert.productNamespace.providedTags.split("\\s*,\\s*")));	// tags can be defined as a comma separated string
					if (rhnDefnitionProductCert.productId.equals(migrationProductCert.productId) &&
//NOT A MUST MATCH		rhnDefnitionProductCert.productName.equals(migrationProductCert.productName) &&
						rhnDefnitionProductCert.productNamespace.version.equals(migrationProductCert.productNamespace.version) &&
						rhnDefnitionProductCertProvidedTags.containsAll(migrationProductCertProvidedTags) && migrationProductCertProvidedTags.containsAll(rhnDefnitionProductCertProvidedTags) &&
						rhnDefnitionProductCert.productNamespace.arch.equals(migrationProductCert.productNamespace.arch) &&
						rhnDefnitionProductCert.productNamespace.brandType==migrationProductCert.productNamespace.brandType) {
						foundNewerVersionOfMigrationProductCert=true;
						Assert.assertTrue(foundNewerVersionOfMigrationProductCert,"Found a functionally equivalent product cert among the current product certs defined by release engineering.  This accounts for above warning.");
						log.info("migrationProductCert:    "+migrationProductCert);
						log.info("rhnDefnitionProductCert: "+rhnDefnitionProductCert);
					}
				}
				
				// skip a known issue that is irrelevant on RHEL7.  This is failing because rcm updated product cert product_ids/rhel-6.7-eus/EUS-Server-s390x-1a3bca323339-73.pem to product_ids/rhel-6.7-eus/EUS-Server-s390x-e9eb3d196edc-73.pem, but this is irrelevant on rhel7
				if (migrationProductCert.file.getPath().equals("/usr/share/rhsm/product/RHEL-7/EUS-Server-s390x-1a3bca323339-73.pem")) {
					//	[root@jsefler-rhel7 rhnDefinitionsDir]# rct cat-cert /usr/share/rhsm/product/RHEL-7/EUS-Server-s390x-1a3bca323339-73.pem | grep -A7 "Product:"
					//	Product:
					//		ID: 73
					//		Name: Red Hat Enterprise Linux for IBM System z - Extended Update Support
					//		Version: 6.7
					//		Arch: s390x
					//		Tags: rhel-6-ibm-system-z
					//		Brand Type: 
					//		Brand Name: 
					log.info("Ignoring the WARNING for migration product cert '"+migrationProductCert.file+"' because it is irrelevant on RHEL7.  It applies to RHEL6: "+ migrationProductCert.productNamespace);
					continue;
				}
				
				// skip a known issue product 126 was removed in commit http://git.app.eng.bos.redhat.com/git/rcm/rcm-metadata.git/commit/product_ids?id=f04a9cfd3dbe971f710afedfecc7f7c8bdf4c307 but was NOT regenerated in http://git.app.eng.bos.redhat.com/git/rcm/rcm-metadata.git/commit/product_ids?id=721aaec8aa57bff38929c6736dc98696b1a89011
				if (migrationProductCert.file.getPath().equals("/usr/share/rhsm/product/RHEL-7/Server-SJIS-x86_64-1d1c6aac9e3b-126.pem")) {
					//	[root@jsefler-rhel7 ~]# rct cat-cert /usr/share/rhsm/product/RHEL-7/Server-SJIS-x86_64-1d1c6aac9e3b-126.pem | grep -A7 "Product:"
					//	Product:
					//		ID: 126
					//		Name: Red Hat S-JIS Support (for RHEL Server)
					//		Version: 7.4
					//		Arch: x86_64
					//		Tags: rhel-7-server-sjis,rhel-7-sjis
					//		Brand Type: 
					//		Brand Name: 
					log.info("Ignoring the WARNING for migration product cert '"+migrationProductCert.file+"' because it is extraneous ad will not harm anything.  It was excluded in the regenerated certs by rcm in commit http://git.app.eng.bos.redhat.com/git/rcm/rcm-metadata.git/commit/product_ids?id=721aaec8aa57bff38929c6736dc98696b1a89011");
					continue;
				}
				
				if (!foundNewerVersionOfMigrationProductCert) verifiedMatchForAllMigrationProductCertFiles = false;
			}
		}
		
		// now assert that all of product certs from the current rhnDefinitions structure are locally available for install
		if (false) {	// NOT A VALID TEST since all product certs from the current rhnDefinitions structure may not be mapped in product-certs.json file 
		for (ProductCert rhnDefinitionProductCert : rhnDefnitionProductCerts) {
			if (migrationProductCerts.contains(rhnDefinitionProductCert)) {
				Assert.assertTrue(true, "CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was found among the local migration product certs available for installation.");
			} else {
				
				// determine if the rhnDefinitionProductCert is not mapped to any RHEL [5|6] RHN Channels defined in the product baseline file
				List<String> rhnChannels = cdnProductBaselineProductIdMap.get(rhnDefinitionProductCert.productId);
				if (rhnChannels==null) {
					log.warning("CDN Product Baseline has an empty list of RHN Channels for Product ID '"+rhnDefinitionProductCert.productId+"' Name '"+rhnDefinitionProductCert.productName+"'.  This could be a rel-eng defect.");
					rhnChannels = new ArrayList<String>();
				}
				Set<String> rhnChannelsFilteredForRhelRelease = new HashSet<String>();
				for (String rhnChannel : rhnChannels) {
					// filter out all RHN Channels not associated with this release  (e.g., assume that an rhn channel containing "-5-" or ends in "-5" is only applicable to rhel5 
					if (!(rhnChannel.contains("-"+clienttasks.redhatReleaseX+"-") || rhnChannel.endsWith("-"+clienttasks.redhatReleaseX))) continue;
					rhnChannelsFilteredForRhelRelease.add(rhnChannel);
				}
				if (rhnChannelsFilteredForRhelRelease.isEmpty()) {
					log.info("CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was NOT found among the current migration product certs.  No RHEL '"+clienttasks.redhatReleaseX+"' RHN Channels in '"+sm_rhnDefinitionsProductBaselineFile+"' map to Product ID '"+rhnDefinitionProductCert.productId+"' Name '"+rhnDefinitionProductCert.productName+"'.");	
				} else {
					log.warning("CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was NOT found among the current migration product certs.  It is probably a new product cert generated by release engineering and therefore subscription-manager-migration-data needs a regeneration.");
					verifiedMatchForAllMigrationProductCertFiles = false;
				}
			}
		}
		}
		
		Assert.assertTrue(verifiedMatchForAllMigrationProductCertFiles,"All of the migration productCerts in directory '"+baseProductsDir+"' match the current ["+sm_rhnDefinitionsGitRepository+"] product certs for this RHEL release '"+clienttasks.redhatReleaseXY+"' ");
	}
	
	
	@Deprecated
	@Test(	description="Verify that all of the required RHN Channels in the ProductBaseline file are accounted for in channel-cert-mapping.txt",
			groups={"Tier3Tests"},
			dependsOnMethods={"testChannelCertMapping"},
			dataProvider="RhnChannelFromProductBaselineData",
			enabled=false)	// 9/9/2013 RHEL65: disabling this test in favor of new testChannelCertMappingFileSupportsRhnChannelFromProductCerts
	//@ImplementsNitrateTest(caseId=)
	public void testChannelCertMappingFileSupportsRhnChannelFromProductBaseline_DEPRECATED(Object bugzilla, String productBaselineRhnChannel, String productBaselineProductId) throws JSONException {
		
		// does the cdn indicate that this channel maps to more than one product?
		if (cdnProductBaselineChannelMap.get(productBaselineRhnChannel).size()>1) {
			log.warning("According to the CDN Product Baseline, RHN Channel '"+productBaselineRhnChannel+"' maps to more than one product id: "+cdnProductBaselineChannelMap.get(productBaselineRhnChannel));
			// handle special cases to decide what productId should be mapped (see bug https://bugzilla.redhat.com/show_bug.cgi?id=786257)
			// SPECIAL CASE 1:	productId:68  productName:Red Hat Enterprise Linux Desktop
			if (Arrays.asList(
					"rhel-x86_64-client-5",
					"rhel-x86_64-client-5-debuginfo",
					"rhel-x86_64-client-5-beta",
					"rhel-x86_64-client-5-beta-debuginfo",
					"rhel-x86_64-client-supplementary-5",
					"rhel-x86_64-client-supplementary-5-debuginfo",
					"rhel-x86_64-client-supplementary-5-beta",
					"rhel-x86_64-client-supplementary-5-beta-debuginfo",
					"rhel-i386-client-5",
					"rhel-i386-client-5-debuginfo",
					"rhel-i386-client-5-beta",
					"rhel-i386-client-5-beta-debuginfo",
					"rhel-i386-client-supplementary-5",
					"rhel-i386-client-supplementary-5-debuginfo",
					"rhel-i386-client-supplementary-5-beta",
					"rhel-i386-client-supplementary-5-beta-debuginfo").contains(productBaselineRhnChannel)) {
				log.warning("However, RHN Channel '"+productBaselineRhnChannel+"' is a special case.  See https://bugzilla.redhat.com/show_bug.cgi?id=786257#c1 for more details.");
				Set<String> productIdsForDesktopAndWorkstation = new HashSet<String>();
				productIdsForDesktopAndWorkstation.add("68");	// rhel-5,rhel-5-client							Red Hat Enterprise Linux Desktop
				productIdsForDesktopAndWorkstation.add("71");	// rhel-5-client-workstation,rhel-5-workstation	Red Hat Enterprise Linux Workstation
				Assert.assertTrue(cdnProductBaselineChannelMap.get(productBaselineRhnChannel).containsAll(productIdsForDesktopAndWorkstation) && productIdsForDesktopAndWorkstation.containsAll(cdnProductBaselineChannelMap.get(productBaselineRhnChannel)),
						"Expecting RHN Channel '"+productBaselineRhnChannel+"' on the CDN Product Baseline to map only to productIds "+productIdsForDesktopAndWorkstation);
				Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productBaselineRhnChannel)),"68",
						"As dictated in the comments of https://bugzilla.redhat.com/show_bug.cgi?id=786257 subscription-manager-migration-data file '"+channelCertMappingFilename+"' should only map RHN Channel '"+productBaselineRhnChannel+"' to productId 68.");
				return;

			// SPECIAL CASE 2:	productId:180  productName:Red Hat Beta rhnChannels:
			} else if (Arrays.asList(	
					"rhel-i386-client-dts-5-beta", 
					"rhel-i386-client-dts-5-beta-debuginfo", 
					"rhel-i386-client-dts-6-beta", 
					"rhel-i386-client-dts-6-beta-debuginfo", 
					"rhel-i386-server-dts-5-beta", 
					"rhel-i386-server-dts-5-beta-debuginfo", 
					"rhel-i386-server-dts-6-beta", 
					"rhel-i386-server-dts-6-beta-debuginfo", 
					"rhel-i386-workstation-dts-6-beta", 
					"rhel-i386-workstation-dts-6-beta-debuginfo", 
					"rhel-x86_64-client-dts-5-beta", 
					"rhel-x86_64-client-dts-5-beta-debuginfo", 
					"rhel-x86_64-client-dts-6-beta", 
					"rhel-x86_64-client-dts-6-beta-debuginfo", 
					"rhel-x86_64-hpc-node-dts-6-beta", 
					"rhel-x86_64-hpc-node-dts-6-beta-debuginfo", 
					"rhel-x86_64-server-dts-5-beta", 
					"rhel-x86_64-server-dts-5-beta-debuginfo", 
					"rhel-x86_64-server-dts-6-beta", 
					"rhel-x86_64-server-dts-6-beta-debuginfo", 
					"rhel-x86_64-workstation-dts-6-beta", 
					"rhel-x86_64-workstation-dts-6-beta-debuginfo").contains(productBaselineRhnChannel)) {
				log.warning("However, RHN Channel '"+productBaselineRhnChannel+"' is a special case.  See https://bugzilla.redhat.com/show_bug.cgi?id=820749#c4 for more details.");
				Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productBaselineRhnChannel)),"180",
						"As dictated in the comments of https://bugzilla.redhat.com/show_bug.cgi?id=820749 subscription-manager-migration-data file '"+channelCertMappingFilename+"' should only map RHN Channel '"+productBaselineRhnChannel+"' to productId 180.");
				return;

			// SPECIAL CASE:	placeholder for next special case
			} else if (false) {
				
			} else {
				Assert.fail("Encountered an unexpected case in the CDN Product Baseline where RHN Channel '"+productBaselineRhnChannel+"' maps to more than one product id: "+cdnProductBaselineChannelMap.get(productBaselineRhnChannel)+".  Do not know how to choose which productId channel '"+productBaselineRhnChannel+"' maps to in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			}
		}
		
		// Special case for High Touch Beta productId 135  reference: https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4
		if (productBaselineProductId.equals("135")) {
			log.warning("For product id "+productBaselineProductId+" (Red Hat Enterprise Linux Server HTB), we actually do NOT want a channel cert mapping as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(productBaselineRhnChannel),
					"CDN Product Baseline RHN Channel '"+productBaselineRhnChannel+"' supporting productId="+productBaselineProductId+" was NOT mapped to a product certificate in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.  This is a special case (Bugzilla 799152#c4).");
			return;
		}
		
		// Special case for High Touch Beta productId 155  reference: https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4
		if (productBaselineProductId.equals("155")) {
			log.warning("For product id "+productBaselineProductId+" (Red Hat Enterprise Linux Workstation HTB), we actually do NOT want a channel cert mapping as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(productBaselineRhnChannel),
					"CDN Product Baseline RHN Channel '"+productBaselineRhnChannel+"' supporting productId="+productBaselineProductId+" was NOT mapped to a product certificate in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.  This is a special case (Bugzilla 799152#c4).");
			return;
		}
		
		// Special case for Red Hat Developer Toolset (for RHEL for IBM POWER) channels *-ppc-*  reference: https://bugzilla.redhat.com/show_bug.cgi?id=869008#c4
		if (productBaselineProductId.equals("195")) {
			if (Arrays.asList(	
					"rhel-ppc-server-dts-5-beta", 
					"rhel-ppc-server-dts-5-beta-debuginfo").contains(productBaselineRhnChannel)) {
				log.warning("DTS for ppc was added at DTS 1.1 Beta but then dropped before 1.1 GA");
				throw new SkipException("DTS for ppc was added at DTS 1.1 Beta but then dropped before 1.1 GA.  Skipping this test for channel '"+productBaselineRhnChannel+"' as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=869008#c4");
			}
		}
		
		// assert that the subscription-manager-migration-data file has a mapping for this RHN Channel found in the CDN Product Baseline
		Assert.assertTrue(channelsToProductCertFilenamesMap.containsKey(productBaselineRhnChannel),
				"CDN Product Baseline RHN Channel '"+productBaselineRhnChannel+"' is accounted for in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
		
		// now assert that the subscription-manager-migration-data mapping for the RHN Channel is to the same productId as mapped in the CDN Product Baseline
		Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productBaselineRhnChannel)), productBaselineProductId,
				"The subscription-manager-migration-data file '"+channelCertMappingFilename+"' maps RHN Channel '"+productBaselineRhnChannel+"' to the same productId as dictated in the CDN Product Baseline.");
	}

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21884", "RHEL7-51739"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Verify that all of the required RHN Channels in the product-certs.json file are accounted for in channel-cert-mapping.txt",
			groups={"Tier3Tests","blockedByBug-1025338","blockedByBug-1080072","blockedByBug-1241221"},
			dependsOnMethods={"testChannelCertMapping"},
			dataProvider="RhnChannelFromProductCertsData",
			enabled=true) // Starting in RHEL65, we are moving away from product-baseline.json and replacing it with product-certs.json
	//@ImplementsNitrateTest(caseId=)
	public void testChannelCertMappingFileSupportsRhnChannelFromProductCerts(Object bugzilla, String productCertsRhnChannel, File productCertsProductFile) throws JSONException {

// UNDER CONSTRUCTION
//		// does the cdn indicate that this channel maps to more than one product?
//		if (cdnProductBaselineChannelMap.get(productCertsRhnChannel).size()>1) {
//			log.warning("According to the CDN Product Baseline, RHN Channel '"+productCertsRhnChannel+"' maps to more than one product id: "+cdnProductBaselineChannelMap.get(productCertsRhnChannel));
//			// handle special cases to decide what productId should be mapped (see bug https://bugzilla.redhat.com/show_bug.cgi?id=786257)
//			// SPECIAL CASE 1:	productId:68  productName:Red Hat Enterprise Linux Desktop
//			if (Arrays.asList(
//					"rhel-x86_64-client-5",
//					"rhel-x86_64-client-5-debuginfo",
//					"rhel-x86_64-client-5-beta",
//					"rhel-x86_64-client-5-beta-debuginfo",
//					"rhel-x86_64-client-supplementary-5",
//					"rhel-x86_64-client-supplementary-5-debuginfo",
//					"rhel-x86_64-client-supplementary-5-beta",
//					"rhel-x86_64-client-supplementary-5-beta-debuginfo",
//					"rhel-i386-client-5",
//					"rhel-i386-client-5-debuginfo",
//					"rhel-i386-client-5-beta",
//					"rhel-i386-client-5-beta-debuginfo",
//					"rhel-i386-client-supplementary-5",
//					"rhel-i386-client-supplementary-5-debuginfo",
//					"rhel-i386-client-supplementary-5-beta",
//					"rhel-i386-client-supplementary-5-beta-debuginfo").contains(productCertsRhnChannel)) {
//				log.warning("However, RHN Channel '"+productCertsRhnChannel+"' is a special case.  See https://bugzilla.redhat.com/show_bug.cgi?id=786257#c1 for more details.");
//				Set<String> productIdsForDesktopAndWorkstation = new HashSet<String>();
//				productIdsForDesktopAndWorkstation.add("68");	// rhel-5,rhel-5-client							Red Hat Enterprise Linux Desktop
//				productIdsForDesktopAndWorkstation.add("71");	// rhel-5-client-workstation,rhel-5-workstation	Red Hat Enterprise Linux Workstation
//				Assert.assertTrue(cdnProductBaselineChannelMap.get(productCertsRhnChannel).containsAll(productIdsForDesktopAndWorkstation) && productIdsForDesktopAndWorkstation.containsAll(cdnProductBaselineChannelMap.get(productCertsRhnChannel)),
//						"Expecting RHN Channel '"+productCertsRhnChannel+"' on the CDN Product Baseline to map only to productIds "+productIdsForDesktopAndWorkstation);
//				Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productCertsRhnChannel)),"68",
//						"As dictated in the comments of https://bugzilla.redhat.com/show_bug.cgi?id=786257 subscription-manager-migration-data file '"+channelCertMappingFilename+"' should only map RHN Channel '"+productCertsRhnChannel+"' to productId 68.");
//				return;
//
//			// SPECIAL CASE 2:	productId:180  productName:Red Hat Beta rhnChannels:
//			} else if (Arrays.asList(	
//					"rhel-i386-client-dts-5-beta", 
//					"rhel-i386-client-dts-5-beta-debuginfo", 
//					"rhel-i386-client-dts-6-beta", 
//					"rhel-i386-client-dts-6-beta-debuginfo", 
//					"rhel-i386-server-dts-5-beta", 
//					"rhel-i386-server-dts-5-beta-debuginfo", 
//					"rhel-i386-server-dts-6-beta", 
//					"rhel-i386-server-dts-6-beta-debuginfo", 
//					"rhel-i386-workstation-dts-6-beta", 
//					"rhel-i386-workstation-dts-6-beta-debuginfo", 
//					"rhel-x86_64-client-dts-5-beta", 
//					"rhel-x86_64-client-dts-5-beta-debuginfo", 
//					"rhel-x86_64-client-dts-6-beta", 
//					"rhel-x86_64-client-dts-6-beta-debuginfo", 
//					"rhel-x86_64-hpc-node-dts-6-beta", 
//					"rhel-x86_64-hpc-node-dts-6-beta-debuginfo", 
//					"rhel-x86_64-server-dts-5-beta", 
//					"rhel-x86_64-server-dts-5-beta-debuginfo", 
//					"rhel-x86_64-server-dts-6-beta", 
//					"rhel-x86_64-server-dts-6-beta-debuginfo", 
//					"rhel-x86_64-workstation-dts-6-beta", 
//					"rhel-x86_64-workstation-dts-6-beta-debuginfo").contains(productCertsRhnChannel)) {
//				log.warning("However, RHN Channel '"+productCertsRhnChannel+"' is a special case.  See https://bugzilla.redhat.com/show_bug.cgi?id=820749#c4 for more details.");
//				Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productCertsRhnChannel)),"180",
//						"As dictated in the comments of https://bugzilla.redhat.com/show_bug.cgi?id=820749 subscription-manager-migration-data file '"+channelCertMappingFilename+"' should only map RHN Channel '"+productCertsRhnChannel+"' to productId 180.");
//				return;
//
//			// SPECIAL CASE:	placeholder for next special case
//			} else if (false) {
//				
//			} else {
//				Assert.fail("Encountered an unexpected case in the CDN Product Baseline where RHN Channel '"+productCertsRhnChannel+"' maps to more than one product id: "+cdnProductBaselineChannelMap.get(productCertsRhnChannel)+".  Do not know how to choose which productId channel '"+productCertsRhnChannel+"' maps to in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
//			}
//		}
//		
//		// Special case for High Touch Beta productId 135  reference: https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4
//		if (productCertsProductId.equals("135")) {
//			log.warning("For product id "+productCertsProductId+" (Red Hat Enterprise Linux Server HTB), we actually do NOT want a channel cert mapping as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4");
//			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(productCertsRhnChannel),
//					"CDN Product Baseline RHN Channel '"+productCertsRhnChannel+"' supporting productId="+productCertsProductId+" was NOT mapped to a product certificate in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.  This is a special case (Bugzilla 799152#c4).");
//			return;
//		}
//		
//		// Special case for High Touch Beta productId 155  reference: https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4
//		if (productCertsProductId.equals("155")) {
//			log.warning("For product id "+productCertsProductId+" (Red Hat Enterprise Linux Workstation HTB), we actually do NOT want a channel cert mapping as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4");
//			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(productCertsRhnChannel),
//					"CDN Product Baseline RHN Channel '"+productCertsRhnChannel+"' supporting productId="+productCertsProductId+" was NOT mapped to a product certificate in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.  This is a special case (Bugzilla 799152#c4).");
//			return;
//		}
		
		// (degregor 5/4/2012) We intentionally exclude HTB channels from the migration script.  It's not a supported use case.
		// See Bug 1011992 - High Touch Beta channel mappings should be excluded from channel-cert-mapping.txt https://bugzilla.redhat.com/show_bug.cgi?id=1011992
		if (productCertsRhnChannel.matches(".+-htb(-.*|$)")) {
			log.warning("(degregor 5/4/2012) We intentionally exclude HTB channels from the migration script.  It's not a supported use case.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(productCertsRhnChannel),
					"CDN Product Certs RHN Channel '"+productCertsRhnChannel+"' is NOT accounted for in the subscription-manager-migration-data file '"+channelCertMappingFilename+"' since it is a High Touch Beta channel.");
			return;
		}
		
		// assert that the subscription-manager-migration-data file has a mapping for this RHN Channel found in the CDN Product Certs
		Assert.assertTrue(channelsToProductCertFilenamesMap.containsKey(productCertsRhnChannel),
				"CDN Product Certs RHN Channel '"+productCertsRhnChannel+"' is accounted for in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
	
		// now assert that the subscription-manager-migration-data mapping for the RHN Channel is to the same product cert file as mapped in the CDN Product Certs
		if (!channelsToProductCertFilenamesMap.get(productCertsRhnChannel).equals(productCertsProductFile.getName())) {
			ProductCert migrationProductCert = clienttasks.getProductCertFromProductCertFile(new File(baseProductsDir+"/"+channelsToProductCertFilenamesMap.get(productCertsRhnChannel)));
			ProductCert rhnDefinitionsProductCert = clienttasks.getProductCertFromProductCertFile(new File (clienttasks.rhnDefinitionsDir+"/product_ids"+productCertsProductFile));
			log.warning("The subscription-manager-migration-data file '"+channelCertMappingFilename+"' maps RHN Channel '"+productCertsRhnChannel+"' to '"+channelsToProductCertFilenamesMap.get(productCertsRhnChannel)+"' which is different than the rhnDefinitions product-certs.json mapping to '"+productCertsProductFile+"'.  Comparing contents for effective equality...");
			log.info("Migration product cert '"+migrationProductCert.file+"':  "+migrationProductCert.productNamespace);
			log.info("CDN Product Cert '"+rhnDefinitionsProductCert.file+"':  "+rhnDefinitionsProductCert.productNamespace);
			log.info("Expecting those to be effectively equal.  If not, then a release-engineering bug is likely.");
			Assert.assertEquals(migrationProductCert.productNamespace.name, rhnDefinitionsProductCert.productNamespace.name, "Comparing productNamespace.name between '"+rhnDefinitionsProductCert.file+"' and '"+migrationProductCert.file+"'");
			Assert.assertEquals(migrationProductCert.productNamespace.id, rhnDefinitionsProductCert.productNamespace.id, "Comparing productNamespace.id between '"+rhnDefinitionsProductCert.file+"' and '"+migrationProductCert.file+"'");
			Assert.assertEquals(migrationProductCert.productNamespace.arch, rhnDefinitionsProductCert.productNamespace.arch, "Comparing productNamespace.arch between '"+rhnDefinitionsProductCert.file+"' and '"+migrationProductCert.file+"'");
			Assert.assertTrue(areTagsEquivalent(migrationProductCert.productNamespace.providedTags, rhnDefinitionsProductCert.productNamespace.providedTags), "The productNamespace.providedTags between '"+rhnDefinitionsProductCert.file+"' '"+rhnDefinitionsProductCert.productNamespace.providedTags+"' and '"+migrationProductCert.file+"' '"+migrationProductCert.productNamespace.providedTags+"' are equivalent.");
			// TEMPORARY WORKAROUND
			if (clienttasks.redhatReleaseXY.equals("7.4") && rhnDefinitionsProductCert.productNamespace.version.startsWith(clienttasks.redhatReleaseXY) && !migrationProductCert.productNamespace.version.equals(rhnDefinitionsProductCert.productNamespace.version)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1436441";	// Bug 1436441 - subscription-manager-migration-data for RHEL7.4 needs RHEL7.4 product certs (not RHEL7.3 certs)
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Skipping assertion that the product cert version provided by subscription-manager-migration-data mapped to RHN channel "+productCertsRhnChannel+" matches the upstream mapping from product-certs.json while bug "+bugId+" is open.");
				}
			}
			// END OF WORKAROUND
			Assert.assertEquals(migrationProductCert.productNamespace.version, rhnDefinitionsProductCert.productNamespace.version, "Comparing productNamespace.version between '"+rhnDefinitionsProductCert.file+"' and '"+migrationProductCert.file+"'");
		} else {
		
			Assert.assertEquals(channelsToProductCertFilenamesMap.get(productCertsRhnChannel), productCertsProductFile.getName(),
				"The subscription-manager-migration-data file '"+channelCertMappingFilename+"' maps RHN Channel '"+productCertsRhnChannel+"' to the same product cert file as dictated in the CDN Product Certs.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20104", "RHEL7-33102"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that all of the classic RHN Channels available to a classically registered consumer are accounted for in the in the channel-cert-mapping.txt or is a known exception",
			groups={"Tier1Tests"},
			dependsOnMethods={"testChannelCertMapping"},
			dataProvider="getRhnClassicBaseAndAvailableChildChannelsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testChannelCertMappingFileSupportsRhnClassicBaseAndAvailableChildChannel(Object bugzilla, String classicRhnChannel) {
		
		// SPECIAL CASES.....
		
		// (degregor 7/1/2014) internal-only channels (can be skipped in mappings): rhel-x86_64-server-xfs-5 rhel-x86_64-server-xfs-5-beta
		if (classicRhnChannel.matches("rhel-.+-server-xfs-5(-.+|$)")) {
			log.warning("(degregor 7/1/2014) internal-only channels (can be skipped in mappings): rhel-x86_64-server-xfs-5 rhel-x86_64-server-xfs-5-beta;  https://bugzilla.redhat.com/show_bug.cgi?id=1105656#c5");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;			
		}
		
		// (degregor 7/1/2014) internal-only channels (can be skipped in mappings): rhel-x86_64-server-5-shadow-debuginfo
		if (classicRhnChannel.matches("rhel-.+-server-5-shadow(-.+|$)")) {
			log.warning("(degregor 7/1/2014) internal-only channels (can be skipped in mappings): rhel-x86_64-server-5-shadow-debuginfo;  https://bugzilla.redhat.com/show_bug.cgi?id=1105656#c5");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;			
		}
		
		// (degregor 7/18/2014) Please skip all channels that have "-shadow" in the label.
		if (classicRhnChannel.matches("rhel-.+-shadow(-.+|$)")) {
			log.warning("(degregor 7/18/2014) \"shadow\" channels are internal-only (can be skipped in channel-to-productCert mappings)");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;			
		}
		
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-cf-ae-1' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-cf-ae-1-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-cf-ae-1-beta-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-cf-ae-1-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// (degregor 5/4/2012) CloudForms Application Engine isn't shipping anytime soon, so we decided to remove the CDN repos.  While the channels are there in RHN, no one has access to them.
		if (classicRhnChannel.matches("rhel-.+-6-cf-ae-1(-.*|$)")) {
			log.warning("(degregor 5/4/2012) CloudForms Application Engine isn't shipping anytime soon, so we decided to remove the CDN repos.  While the channels are there in RHN, no one has access to them.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-htb' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-htb-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.827 - WARNING: RHN Classic channel 'sam-rhel-x86_64-server-6-htb' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.828 - WARNING: RHN Classic channel 'sam-rhel-x86_64-server-6-htb-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// (degregor 5/4/2012) We intentionally exclude HTB channels from the migration script.  It's not a supported use case.
		if (classicRhnChannel.matches(".+-htb(-.*|$)")) {
			log.warning("(degregor 5/4/2012) We intentionally exclude HTB channels from the migration script.  It's not a supported use case.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		// 201205032049:22.819 - WARNING: RHN Classic channel 'rhel-x86_64-server-clusteredstorage-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.820 - WARNING: RHN Classic channel 'rhel-x86_64-server-ei-replication-6' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.820 - WARNING: RHN Classic channel 'rhel-x86_64-server-ei-replication-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.820 - WARNING: RHN Classic channel 'rhel-x86_64-server-ei-replication-6-beta-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.820 - WARNING: RHN Classic channel 'rhel-x86_64-server-ei-replication-6-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'. 
		// (degregor 5/4/2012) The above channels aren't used.
		if (classicRhnChannel.matches("rhel-.+-ei-replication-6(-.*|$)")  || classicRhnChannel.matches("rhel-.+-clusteredstorage-6(-.*|$)")) {
			log.warning("(degregor 5/4/2012) The above channels aren't used.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		// 201205032049:22.827 - WARNING: RHN Classic channel 'rhn-tools-rhel-x86_64-server-6' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.827 - WARNING: RHN Classic channel 'rhn-tools-rhel-x86_64-server-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.827 - WARNING: RHN Classic channel 'rhn-tools-rhel-x86_64-server-6-beta-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.827 - WARNING: RHN Classic channel 'rhn-tools-rhel-x86_64-server-6-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// (degregor 5/4/2012) RHN Tools content doesn't get delivered through CDN.
		/* (degregor 7/17/2014) RHN Tools was added to CDN in May
		if (classicRhnChannel.startsWith("rhn-tools-rhel-")) {
			log.warning("(degregor 5/4/2012) RHN Tools content doesn't get delivered through CDN.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		*/
		
		// 201205080442:43.007 - WARNING: RHN Classic channel 'rhel-x86_64-server-highavailability-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205080442:43.008 - WARNING: RHN Classic channel 'rhel-x86_64-server-largefilesystem-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205080442:43.010 - WARNING: RHN Classic channel 'rhel-x86_64-server-loadbalance-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// (degregor 5/8/2012) These channels are not used and can be ignored.
		if (classicRhnChannel.matches("rhel-.+-highavailability-6-beta") || classicRhnChannel.matches("rhel-.+-largefilesystem-6-beta") || classicRhnChannel.matches("rhel-.+-loadbalance-6-beta")) {
			log.warning("(degregor 5/8/2012) These channels are not used and can be ignored.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		// 201205080556:10.326 - WARNING: RHN Classic channel 'rhel-x86_64-server-hts-6' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205080556:10.326 - WARNING: RHN Classic channel 'rhel-x86_64-server-hts-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		//	RHN Classic channel 'rhel-x86_64-server-hts-5' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt'.
		//	RHN Classic channel 'rhel-x86_64-server-hts-5-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt'.
		//	RHN Classic channel 'rhel-x86_64-server-hts-5-beta-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt'.
		//	RHN Classic channel 'rhel-x86_64-server-hts-5-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt'.
		// (degregor 5/8/2012) We're not delivering Hardware Certification (aka hts) bits through the CDN at this point.
		/* (degregor 9/30/2013) I do see product 199 mapped to a number of SKUs, so we should include those channels/certs in the migration.
		if (classicRhnChannel.matches("rhel-.+-hts-"+clienttasks.redhatReleaseX+"(-.*|$)")) {
			log.warning("(degregor 5/8/2012) We're not delivering Hardware Certification (aka hts) bits through the CDN at this point.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		*/
		
		// RHN Classic channel 'rhel-x86_64-server-6-rhui-2' is accounted for in subscription-manager-migration-data file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'. expected:<true> but was:<false>
		// RHN Classic channel 'rhel-x86_64-server-6-rhui-2-debuginfo' is accounted for in subscription-manager-migration-data file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'. expected:<true> but was:<false>
		// https://bugzilla.redhat.com/show_bug.cgi?id=819089#c2
		// https://bugzilla.redhat.com/show_bug.cgi?id=819089#c3
		if (classicRhnChannel.matches("rhel-.+-rhui-2(-.*|$)")) {
			log.warning("(jgregusk 12/11/2012) Migrating a RHUI installation from Classic is not a supported (or even valid) use case.  See https://bugzilla.redhat.com/show_bug.cgi?id=819089#c3");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		if (classicRhnChannel.matches("rhel-.+-server-5-mrg-.*")) {	// rhel-x86_64-server-5-mrg-grid-1 rhel-x86_64-server-5-mrg-grid-1-beta rhel-x86_64-server-5-mrg-grid-2 rhel-x86_64-server-5-mrg-grid-execute-1 rhel-x86_64-server-5-mrg-grid-execute-1-beta rhel-x86_64-server-5-mrg-grid-execute-2 etc.
			if (!classicRhnChannel.matches("rhel-.+-server-5-mrg-messaging-2")) {	// rhel-i386-server-5-mrg-messaging-2 rhel-x86_64-server-5-mrg-messaging-2	// https://bugzilla.redhat.com/show_bug.cgi?id=840102#c2
				// Bug 840102 - channels for rhel-<ARCH>-server-5-mrg-* are not yet mapped to product certs in rcm/rcm-metadata.git
				log.warning("(degregor 8/4/2012) RHEL 5 MRG isn't currently supported in CDN (outside of RHUI) - https://bugzilla.redhat.com/show_bug.cgi?id=840102#c1");
				Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
				return;
			}
		}
		if (classicRhnChannel.matches("rhel-.+-server-hpc-5(-.*|$)")) {	// rhel-x86_64-server-hpc-5-beta
			// Bug 840103 - channel for rhel-x86_64-server-hpc-5-beta is not yet mapped to product cert in rcm/rcm-metadata.git
			log.warning("(degregor 8/4/2012) The RHEL 5 HPC products is not currently supported in CDN - https://bugzilla.redhat.com/show_bug.cgi?id=840103#c1");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.matches("rhel-.+-server-rhev-hdk-2-5(-.+|$)")) {	// rhel-x86_64-server-rhev-hdk-2-5 rhel-x86_64-server-rhev-hdk-2-5-beta
			// Bug 840108 - channels for rhel-<ARCH>-rhev-hdk-2-5-* are not yet mapped to product certs in rcm/rhn-definitions.git
			log.warning("(degregor 8/4/2012) RHEV H Dev Kit is not currently supported in CDN - https://bugzilla.redhat.com/show_bug.cgi?id=840108#c1");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.startsWith("rhx-")) {	// rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5 rhx-amanda-enterprise-backup-2.6-rhel-x86_64-server-5 etcetera
			// Bug 840111 - various rhx channels are not yet mapped to product certs in rcm/rcm-metadata.git 
			log.warning("(degregor 8/4/2012) RHX products are not currently supported in CDN - https://bugzilla.redhat.com/show_bug.cgi?id=840111#c2");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.matches("rhel-.+-client-dts-5(-.*|$)")) {	// rhel-i386-client-dts-5-debuginfo rhel-i386-client-dts-5-beta-debuginfo rhel-i386-client-dts-5-beta rhel-i386-client-dts-5
			// Bug 969160 - rhel-*-client-dts-5* channels are not mapped in channel-cert-mapping.txt
			// Bug 969156 - RHN Channels: [] in product-baseline.json is empty for "Red Hat Developer Toolset (for RHEL Client)"
			log.warning("(degregor 5/31/2013) DTS for Client got dropped.  Those channels shouldn't be available in RHN. - https://bugzilla.redhat.com/show_bug.cgi?id=969156#c1");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.matches("rhel-.+-client-dts-6(-.*|$)")) {	// rhel-x86_64-client-dts-6-beta-debuginfo rhel-x86_64-client-dts-6-beta rhel-x86_64-client-dts-6-debuginfo rhel-x86_64-client-dts-6 rhel-i386-client-dts-6-beta rhel-i386-client-dts-6-beta-debuginfo rhel-i386-client-dts-6-debuginfo rhel-i386-client-dts-6
			log.warning("(degregor 9/17/2013) rhel-x86_64-client-dts-6* is no longer used.  please ignore those.  I'll make a note to remove them from stage.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.matches("rhel-.+-client-multimedia-5(-.*|$)")) {	// rhel-i386-client-multimedia-5 rhel-i386-client-multimedia-5-beta
			log.warning("(degregor 5/31/2013) I don't think we ever added these to CDN.  Please ignore them for now.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.startsWith("rhel-x86_64-client-6-rhscl-1")) {	// rhel-x86_64-client-6-rhscl-1 rhel-x86_64-client-6-rhscl-1-debuginfo rhel-x86_64-client-6-rhscl-1-beta rhel-x86_64-client-6-rhscl-1-beta-debuginfo
			// Bug 1009071 - the RHN Classic rhel-x86_64-client-6-rhscl-1 channels are not accounted for in product-certs.json
			log.warning("(degregor 10/9/2013) RHSCL on client has been dropped - https://bugzilla.redhat.com/show_bug.cgi?id=1009071#c1");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.startsWith("rhel-x86_64-server-6-cf-me-2")) {
			// Bug 1021664 - Red Hat CloudForms rhel-x86_64-server-6-cf-me-2 channel mappings are missing 
			log.warning("(degregor 07/29/2014) It turns out that the cf-me-2 for RHEL 6 channels were never actually used.  When CF ME was made available they went straight to cf-me-3.  I've gone and moved the rhel-x86_64-server-6-cf-me-2* channels into the shadow channel family and we can ignore them in the migration data.  https://bugzilla.redhat.com/show_bug.cgi?id=1021664#c3");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.endsWith("-hts-7-beta") ||
			classicRhnChannel.endsWith("-hts-7-beta-debuginfo") ||
			classicRhnChannel.endsWith("-v2vwin-7-beta") ||
		    classicRhnChannel.endsWith("-v2vwin-7-beta-debuginfo")) {
			// Bug 1257212 - various RHEL7 channel maps to product certs are missing in subscription-manager-migration-data
			log.warning("(anthomas 09/04/15) Any hts-7-beta and v2v-7-beta channels will be ignored as they do not have any beta content. Please ignore in the future.  https://bugzilla.redhat.com/show_bug.cgi?id=1257212#c6");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.equals("rhel-x86_64-server-6-cf-me-3-beta") ||
			classicRhnChannel.equals("rhel-x86_64-server-6-cf-me-3-beta-debuginfo")) {
			// Bug 1127880 - rhel-x86_64-server-6-cf-me-3-beta channel maps are missing from channel-cert-mapping.txt
			log.warning("(anthomas 11/04/2014) These channels are requested to remain disabled. If possible please skip these in future tests for the time being.  https://bugzilla.redhat.com/show_bug.cgi?id=1127880#c8");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.equals("rhel-x86_64-server-6-rhevm-3-beta") ||
			classicRhnChannel.equals("rhel-x86_64-server-6-rhevm-3-beta-debuginfo")) {
			//	[root@rhsm-sat5 ~]# grep -- 6-rhevm-3 allchannels.txt 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3              153 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3-beta           0		<====  NO CONTENT
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3-beta-debuginfo    0	<====  NO CONTENT
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3-debuginfo      8 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.1            107 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.1-debuginfo    0 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.2            182 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.2-debuginfo    9 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.3            165 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.3-debuginfo    8 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.4            232 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.4-debuginfo   11 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.5            311 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.5-debuginfo   21 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.6            250 
			//	12:24:47       p rhel-x86_64-server-6-rhevm-3.6-debuginfo   14 
			log.warning("RHN channel '"+classicRhnChannel+"' has no content as determined by a satellite-sync --list-channels.  Therefore we should assert that there is no channel mapping in subscription-manager-migration-data because there is no need for one.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		/* commented out in favor of bug https://bugzilla.redhat.com/show_bug.cgi?id=1105656#c5
		if (classicRhnChannel.startsWith("rhel-x86_64-server-productivity-5-beta")) {	// rhel-x86_64-server-productivity-5-beta rhel-x86_64-server-productivity-5-beta-debuginfo
			if (!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel)) {
				String nonBetaClassicRhnChannel = classicRhnChannel.replace("-beta", "");
				if (channelsToProductCertFilenamesMap.containsKey(nonBetaClassicRhnChannel)) {
					Assert.assertTrue(channelsToProductCertFilenamesMap.containsKey(nonBetaClassicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file; however, its non-beta equivalent '"+nonBetaClassicRhnChannel+"' is accounted for in subscription-manager-migration-data file.  Assuming sufficiency.");
					return;
				}
			}
		}
		*/
		
		if (classicRhnChannel.equals("rhel-x86_64-server-6-cf-ce-1-beta") ||
			classicRhnChannel.equals("rhel-x86_64-server-6-cf-ce-1-beta-debuginfo")) {
			// Bug 1299620 - rhel-x86_64-server-6-cf-ce-1-beta channel maps are absent from channel-cert-mapping.txt
			log.warning("(anthomas 02/07/2017) All CF-SE, CF-AE, CF-CE, CF-Tools, and CF-ME-2 are all EOL and not supported. We can probably ignore these mappings safely.  https://bugzilla.redhat.com/show_bug.cgi?id=1299620#c3");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.equals("rhel-x86_64-server-6-cf-se-1-beta") ||
			classicRhnChannel.equals("rhel-x86_64-server-6-cf-se-1-beta-debuginfo")) {
			// Bug 1299621 - rhel-x86_64-server-6-cf-se-1-beta channel maps are absent from channel-cert-mapping.txt
			log.warning("(anthomas 02/07/2017) All CF-SE, CF-AE, CF-CE, CF-Tools, and CF-ME-2 are all EOL and not supported. We can probably ignore these mappings safely.  https://bugzilla.redhat.com/show_bug.cgi?id=1299621#c3");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.equals("rhel-x86_64-server-6-cf-tools-1-beta") ||
			classicRhnChannel.equals("rhel-x86_64-server-6-cf-tools-1-beta-debuginfo") ||
			classicRhnChannel.equals("rhel-i386-server-6-cf-tools-1-beta") ||
			classicRhnChannel.equals("rhel-i386-server-6-cf-tools-1-beta-debuginfo")) {
			// Bug 1299623 - rhel-x86_64-server-6-cf-tools-1-beta channel maps are absent from channel-cert-mapping.txt
			log.warning("(anthomas 02/07/2017) All CF-SE, CF-AE, CF-CE, CF-Tools, and CF-ME-2 are all EOL and not supported. We can probably ignore these mappings safely.  https://bugzilla.redhat.com/show_bug.cgi?id=1299623#c7");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-7-beta") ||
			classicRhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-7-beta-debuginfo")) {
			// Bug 1435245 - RHN channels to product cert maps for "rhel-x86_64-rhev-mgmt-agent-7-beta*" disappeared
			log.warning("(khowell 05/15/2017) We don't necessarily commit to supporting beta migration.  https://bugzilla.redhat.com/show_bug.cgi?id=1435245#c5");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.equals("rhel-x86_64-server-7-rhevh-beta") ||
			classicRhnChannel.equals("rhel-x86_64-server-7-rhevh-beta-debuginfo")) {
			// Bug 1435255 - RHN channels to product cert maps for "rhel-x86_64-server-7-rhevh-beta*" disappeared
			log.warning("(khowell 05/15/2017) We don't necessarily commit to supporting beta migration.  https://bugzilla.redhat.com/show_bug.cgi?id=1435255#c4");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		if (classicRhnChannel.equals("rhel-x86_64-server-6-rhevm-3.6") ||
			classicRhnChannel.equals("rhel-x86_64-server-6-rhevm-3.6-debuginfo")) {
			// Bug 1320592 - rhel-x86_64-server-6-rhevm-3.6 channel maps are absent from channel-cert-mapping.txt 
			log.warning("(jkurik 12/06/2017) This issue does not meet the inclusion criteria for the Production 3 Phase and will be marked as CLOSED/WONTFIX. https://bugzilla.redhat.com/show_bug.cgi?id=1320592#c5");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		if (classicRhnChannel.equals("rhel-x86_64-server-6-rh-gluster-3-nfs") ||
			classicRhnChannel.equals("rhel-x86_64-server-6-rh-gluster-3-nfs-debuginfo")) {
			// Bug 1393557 - rhel-x86_64-server-6-rh-gluster-3-nfs channel maps are absent from channel-cert-mapping.txt
			log.warning("(jkurik 12/06/2017) This issue does not meet the inclusion criteria for the Production 3 Phase and will be marked as CLOSED/WONTFIX. https://bugzilla.redhat.com/show_bug.cgi?id=1393557#c4");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		if (classicRhnChannel.equals("rhel-x86_64-server-6-ost-3-cts") ||
			classicRhnChannel.equals("rhel-x86_64-server-6-ost-3-cts-debuginfo")) {
			// Bug 1232465 - rhel-x86_64-server-6-ost-3-cts channel maps are missing from channel-cert-mapping.txt
			log.warning("(jkurik 12/06/2017) This issue does not meet the inclusion criteria for the Production 3 Phase and will be marked as CLOSED/WONTFIX. https://bugzilla.redhat.com/show_bug.cgi?id=1232465#c5");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		// skip all missing beta channels and ignore their absence from the migration data (as generally agreed upon by dev/qe/pm team)
		if (!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel) && 
			(classicRhnChannel.contains("-beta-")||classicRhnChannel.endsWith("-beta"))) {
			log.warning("Available RHN Classic beta channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			throw new SkipException("Skipping this failed test instance in favor of RFE Bug https://bugzilla.redhat.com/show_bug.cgi?id=1437233 that will intensionally exclude beta channels to product cert mappings.  Also referencing precedence bugs that have been CLOSED WONTFIX on missing beta channels: https://bugzilla.redhat.com/buglist.cgi?bug_id=1437233,1299623,1299621,1299620,1127880");
		}
		
		Assert.assertTrue(channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "RHN Classic channel '"+classicRhnChannel+"' is accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20099", "RHEL7-51106"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify that the channel-cert-mapping.txt does NOT contain any High Touch Beta channel mappings",
			groups={"Tier2Tests","blockedByBug-1011992"},
			dependsOnMethods={"testChannelCertMappingFileExists"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testChannelCertMappingDoesNotContainHighTouchBetaChannels() {
		Assert.assertTrue(RemoteFileTasks.testExists(client, channelCertMappingFilename),"The expected channel cert mapping file '"+channelCertMappingFilename+"' exists.");
		
		SSHCommandResult sshCommandResult = client.runCommandAndWait("grep -- -htb "+channelCertMappingFilename);
		Assert.assertTrue(sshCommandResult.getStdout().trim().isEmpty(), "There should be no hits on htb channels in "+channelCertMappingFilename);
		Assert.assertEquals(sshCommandResult.getStderr().trim(),"", "Stderr from call to grep.");
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(1), "Stderr from call to grep.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20105", "RHEL7-55203"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that the expected RHN base channels supporting this system's RHEL release version are mapped to product certs whose version matches this system's RHEL release",
			groups={"Tier1Tests","blockedByBug-1110863","blockedByBug-1148110","blockedByBug-1197864","blockedByBug-1300766","blockedByBug-1222712","blockedByBug-1228387","blockedByBug-1241221","blockedByBug-1328579","blockedByBug-1328609","blockedByBug-1366747","blockedByBug-1393573","blockedByBug-1436441","blockedByBug-1510200","blockedByBug-1555913"},
			dependsOnMethods={"testChannelCertMapping"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testExpectedBaseChannelsSupportThisSystemsRhelVersion() {
		
		// TEMPORARY WORKAROUND FOR BUG
		if (clienttasks.redhatReleaseXY.equals("7.5")) {
			String bugId = "1549863";	// Bug 1549863 - Missing base RHN-to-ProductCert channel map for "rhel-ppc64le-server-7" and "rhel-aarch64-server-7"
			boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Skipping this test on '"+clienttasks.redhatReleaseXY+"' while bug '"+bugId+"' is open.");
			}
		}
		// END OF WORKAROUND
		
		List<String> expectedBaseChannels = new ArrayList<String>();
		if (clienttasks.redhatReleaseX.equals("7")) {
			// TEMPORARY WORKAROUND FOR BUG
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId1="1078527";	// Bug 1078527 - channel-cert-mapping for ComputeNode rhel-7 product certs are missing and wrong
			String bugId2="1078530";	// Bug 1078530 - product-certs.json appears to contain bad/missing mappings for ComputeNode rhel7 channels
			invokeWorkaroundWhileBugIsOpen = false;
			try {if (invokeWorkaroundWhileBugIsOpen&&(BzChecker.getInstance().isBugOpen(bugId1)||BzChecker.getInstance().isBugOpen(bugId2))) {log.fine("Invoking workaround for Bugzillas:  https://bugzilla.redhat.com/show_bug.cgi?id="+bugId1+" https://bugzilla.redhat.com/show_bug.cgi?id="+bugId2);SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId1);SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId2);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Skipping this test on '"+clienttasks.redhatReleaseX+"' while bug "+bugId1+" or "+bugId2+" is open.");
			}
			// END OF WORKAROUND
			expectedBaseChannels.add("rhel-x86_64-client-7");		// 68	Red Hat Enterprise Linux Desktop
			expectedBaseChannels.add("rhel-x86_64-server-7");		// 69	Red Hat Enterprise Linux Server
			expectedBaseChannels.add("rhel-x86_64-workstation-7");	// 71	Red Hat Enterprise Linux Workstation
			expectedBaseChannels.add("rhel-x86_64-hpc-node-7");		// 76	Red Hat Enterprise Linux for Scientific Computing
			expectedBaseChannels.add("rhel-s390x-server-7");		// 72	Red Hat Enterprise Linux for IBM System z
			expectedBaseChannels.add("rhel-ppc64-server-7");		// 74	Red Hat Enterprise Linux for IBM POWER
			if (Float.valueOf(clienttasks.redhatReleaseXY) >= 7.3) {	// TODO may need updates after bug 1549863 is resolved
			expectedBaseChannels.add("rhel-ppc64le-server-7");		// 279	Red Hat Enterprise Linux for Power, little endian
			expectedBaseChannels.add("rhel-aarch64-server-7");		// 294	Red Hat Enterprise Linux Server for ARM Development Preview <= Preview?
			}
			} else
		if (clienttasks.redhatReleaseX.equals("6")) {
//			// TEMPORARY WORKAROUND FOR BUG
//			String bugId = "0000"; boolean invokeWorkaroundWhileBugIsOpen = true;
//			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
//			if (invokeWorkaroundWhileBugIsOpen) {
//				throw new SkipException("Skipping this test on '"+clienttasks.redhatReleaseX+"' while bug '"+bugId+"' is open.");
//			}
//			// END OF WORKAROUND
			expectedBaseChannels.add("rhel-i386-client-6");			// 68	Red Hat Enterprise Linux Desktop
			expectedBaseChannels.add("rhel-i386-server-6");			// 69	Red Hat Enterprise Linux Server
			expectedBaseChannels.add("rhel-i386-workstation-6");	// 71	Red Hat Enterprise Linux Workstation
			expectedBaseChannels.add("rhel-x86_64-client-6");		// 68	Red Hat Enterprise Linux Desktop
			expectedBaseChannels.add("rhel-x86_64-server-6");		// 69	Red Hat Enterprise Linux Server
			expectedBaseChannels.add("rhel-x86_64-workstation-6");	// 71	Red Hat Enterprise Linux Workstation
			expectedBaseChannels.add("rhel-s390x-server-6");		// 72	Red Hat Enterprise Linux for IBM System z
			expectedBaseChannels.add("rhel-ppc64-server-6");		// 74	Red Hat Enterprise Linux for IBM POWER
			expectedBaseChannels.add("rhel-x86_64-hpc-node-6");		// 76	Red Hat Enterprise Linux for Scientific Computing
		} else
		if (clienttasks.redhatReleaseX.equals("5")) {
//			// TEMPORARY WORKAROUND FOR BUG
//			String bugId = "0000"; boolean invokeWorkaroundWhileBugIsOpen = true;
//			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
//			if (invokeWorkaroundWhileBugIsOpen) {
//				throw new SkipException("Skipping this test on '"+clienttasks.redhatReleaseX+"' while bug '"+bugId+"' is open.");
//			}
//			// END OF WORKAROUND
			expectedBaseChannels.add("rhel-i386-client-5");					// 68	Red Hat Enterprise Linux Desktop
			expectedBaseChannels.add("rhel-i386-server-5");					// 69	Red Hat Enterprise Linux Server
			expectedBaseChannels.add("rhel-i386-client-workstation-5");		// 71	Red Hat Enterprise Linux Workstation
			expectedBaseChannels.add("rhel-x86_64-client-5");				// 68	Red Hat Enterprise Linux Desktop
			expectedBaseChannels.add("rhel-x86_64-server-5");				// 69	Red Hat Enterprise Linux Server
			expectedBaseChannels.add("rhel-x86_64-client-workstation-5");	// 71	Red Hat Enterprise Linux Workstation
			expectedBaseChannels.add("rhel-ia64-server-5");					// 69	Red Hat Enterprise Linux Server
			expectedBaseChannels.add("rhel-s390x-server-5");				// 72	Red Hat Enterprise Linux for IBM System z
			expectedBaseChannels.add("rhel-ppc-server-5");					// 74	Red Hat Enterprise Linux for IBM POWER
		}
		if (expectedBaseChannels.isEmpty()) Assert.fail("Do not know the expected RHN Base Channels for this test.");
		
		// verify all expectedBaseChannels are accounted for in the channel mapping file
		boolean allExpectedBaseChannelsMapped = true;
		for (String expectedBaseChannel : expectedBaseChannels) {
			if (channelsToProductCertFilenamesMap.containsKey(expectedBaseChannel)) {
				Assert.assertTrue(channelsToProductCertFilenamesMap.containsKey(expectedBaseChannel),"RHN Channel mapping file '"+channelCertMappingFilename+"' accounts for RHN base channel '"+expectedBaseChannel+"'.");
			} else {
				log.warning("RHN Channel mapping file '"+channelCertMappingFilename+"' does NOT account for RHN base channel '"+expectedBaseChannel+"'.");
				allExpectedBaseChannelsMapped = false;
			}
		}
		
		// NOTE: This test block is also covered by VerifyRhnRhelChannelsProductCertSupportThisSystemsRhelVersion_Test (Keeping it here for completeness)
		// verify all of the expectedBaseChannels are mapped to a product cert that supports this system's dot release of RHEL
		boolean allBaseChannelProductCertsMatchThisRhelRelease = true;
		for (String expectedBaseChannel : expectedBaseChannels) {
			if (channelsToProductCertFilenamesMap.containsKey(expectedBaseChannel)) {
				if (channelsToProductCertFilenamesMap.get(expectedBaseChannel).equalsIgnoreCase("none")) {
					log.warning("RHN RHEL Base Channel '"+expectedBaseChannel+"' should NOT be mapped to none in mapping file '"+channelCertMappingFilename+"'.");
					allBaseChannelProductCertsMatchThisRhelRelease = false;
				} else {
					ProductCert migrationProductCert = clienttasks.getProductCertFromProductCertFile(new File(baseProductsDir+"/"+channelsToProductCertFilenamesMap.get(expectedBaseChannel)));
					if (clienttasks.redhatReleaseXY.equals(migrationProductCert.productNamespace.version)) {
						Assert.assertEquals(clienttasks.redhatReleaseXY, migrationProductCert.productNamespace.version,"RHN RHEL Base Channel '"+expectedBaseChannel+"' maps to the following product cert that matches RHEL dot release '"+clienttasks.redhatReleaseXY+"': "+migrationProductCert);
					} else {
						log.warning("RHN RHEL Base Channel '"+expectedBaseChannel+"' maps to the following product cert that does NOT match RHEL dot release '"+clienttasks.redhatReleaseXY+"': "+migrationProductCert);
						allBaseChannelProductCertsMatchThisRhelRelease = false;
					}
				}
			}
		}
		
		if (!allExpectedBaseChannelsMapped) Assert.fail("Review logged warnings above for expected RHN base channels that are not mapped.");
		if (!allBaseChannelProductCertsMatchThisRhelRelease) Assert.fail("Review logged warnings above for expected RHN base channel product cert versions that do not match this system.s dot release.");
	}

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20107", "RHEL7-51112"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that the expected RHN RHEL channels supporting this system's RHEL release X.Y version are mapped to product certs whose version matches this system's RHEL release X.Y (also asserts beta channels to Beta product certs)",
			groups={"Tier1Tests","blockedByBug-1080072","blockedByBug-1110863","blockedByBug-1148110","blockedByBug-1197864","blockedByBug-1300766","blockedByBug-1222712","blockedByBug-1241221","blockedByBug-1328579","blockedByBug-1393573","blockedByBug-1436441","blockedByBug-1510200","blockedByBug-1555913"},
			dataProvider="RhnRhelChannelsFromChannelMappingData",
			dependsOnMethods={"testChannelCertMapping"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhnRhelChannelsProductCertSupportThisSystemsRhelVersion(Object bugzilla, String rhnRhelChannel) {
		Assert.assertTrue(!channelsToProductCertFilenamesMap.get(rhnRhelChannel).equalsIgnoreCase("none"), "RHN RHEL Channel '"+rhnRhelChannel+"' does not map to None.");
		ProductCert rhnRhelProductCert = clienttasks.getProductCertFromProductCertFile(new File(baseProductsDir+"/"+channelsToProductCertFilenamesMap.get(rhnRhelChannel)));
		if (rhnRhelChannel.contains(/*clienttasks.redhatReleaseX+*/"-beta-") || rhnRhelChannel.endsWith(/*clienttasks.redhatReleaseX+*/"-beta")) {
			String expectedProductNamespaceVersion = clienttasks.redhatReleaseXY+" Beta";
			if (!rhnRhelProductCert.productNamespace.version.equals(expectedProductNamespaceVersion)) {
				log.warning("RHN RHEL Beta Channel '"+rhnRhelChannel+"' maps to the following product cert that should correspond to this RHEL minor release (expected='"+expectedProductNamespaceVersion+"' actual='"+rhnRhelProductCert.productNamespace.version+"'): "+rhnRhelProductCert.productNamespace);
				throw new SkipException("Skipping this failed test instance in favor of RFE Bug https://bugzilla.redhat.com/show_bug.cgi?id=1437233 that will intensionally exclude beta channels to product cert mappings.  Also referencing precedence bugs that have been CLOSED WONTFIX on beta channels: 1435255 1435245");
			}
			Assert.assertEquals(rhnRhelProductCert.productNamespace.version, expectedProductNamespaceVersion, "RHN RHEL Beta Channel '"+rhnRhelChannel+"' maps to the following product cert that corresponds to this RHEL minor release '"+clienttasks.redhatReleaseXY+"': "+rhnRhelProductCert.productNamespace);			
		} else {
			Assert.assertEquals(rhnRhelProductCert.productNamespace.version, clienttasks.redhatReleaseXY, "RHN RHEL Channel '"+rhnRhelChannel+"' maps to the following product cert that corresponds to this RHEL minor release '"+clienttasks.redhatReleaseXY+"': "+rhnRhelProductCert.productNamespace);
		}
	}
	@DataProvider(name="RhnRhelChannelsFromChannelMappingData")
	public Object[][] getRhnRhelChannelsFromChannelMappingDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnRhelChannelsFromChannelMappingDataAsListOfLists());
	}
	public List<List<Object>> getRhnRhelChannelsFromChannelMappingDataAsListOfLists() throws JSONException {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		for (String rhnChannel : channelsToProductCertFilenamesMap.keySet()) {
			ProductCert rhnRhelProductCert = null;
			if (channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")) rhnRhelProductCert = clienttasks.getProductCertFromProductCertFile(new File(baseProductsDir+"/"+channelsToProductCertFilenamesMap.get(rhnChannel)));
			
			// get all of the provided tags from the rhnRhelProductCert
			List<String> rhnRhelProductProvidedTags = new ArrayList<String>();
			if (rhnRhelProductCert != null) {
				if (rhnRhelProductCert.productNamespace.providedTags != null) {
					for (String providedTag : rhnRhelProductCert.productNamespace.providedTags.split("\\s*,\\s*")) {
						rhnRhelProductProvidedTags.add(providedTag);
					}
				}
			}
			
			// consider skipping channels that do NOT provide the base release rhel-X tag
			if (!rhnRhelProductProvidedTags.contains("rhel-"+clienttasks.redhatReleaseX)) {
				
				// skip non-RHEL channels that do not apply to clienttasks.redhatReleaseX
				//	rhel-x86_64-workstation-6
				//	rhel-x86_64-workstation-6-beta
				//	rhel-x86_64-workstation-6-beta-debuginfo
				//	rhel-x86_64-workstation-6-debuginfo
				if (!rhnChannel.startsWith("rhel-")) continue;
				if (!rhnChannel.endsWith("-"+clienttasks.redhatReleaseX) && !rhnChannel.contains("-"+clienttasks.redhatReleaseX+"-")) continue;
				
				// skip Red Hat OpenShift Enterprise channels
				//	rhel-x86_64-server-6-ose-2.0-rhc
				//	rhel-x86_64-server-6-osop-1-rhc
				if (rhnChannel.contains("-ose-")) continue;
				if (rhnChannel.contains("-osop-")) continue;
				
				// skip Red Hat OpenStack channels
				//	rhel-x86_64-server-6-ost-3
				if (rhnChannel.contains("-ost-")) continue;
				
				// skip Red Hat Enterprise MRG Messaging
				//	rhel-i386-server-6-mrg-messaging-2
				if (rhnChannel.contains("-mrg-messaging-")) continue;
				
				// skip MRG Realtime
				//	rhel-x86_64-server-6-mrg-realtime-2
				if (rhnChannel.contains("-mrg-realtime-")) continue;
				
				// skip MRG Grid
				// rhel-x86_64-hpc-node-6-mrg-grid-2-debuginfo
				// rhel-x86_64-hpc-node-6-mrg-grid-2
				// rhel-x86_64-hpc-node-6-mrg-grid-execute-2-debuginfo
				// rhel-x86_64-hpc-node-6-mrg-grid-execute-2
				// rhel-x86_64-server-6-mrg-grid-execute-2
				// rhel-x86_64-server-6-mrg-grid-execute-2-debuginfo
				if (rhnChannel.contains("-mrg-grid-")) continue;
				
				// skip MRG Management
				// rhel-x86_64-hpc-node-6-mrg-management-2-debuginfo
				// rhel-x86_64-hpc-node-6-mrg-management-2
				// rhel-x86_64-server-6-mrg-management-2-debuginfo
				// rhel-x86_64-server-6-mrg-management-2
				if (rhnChannel.contains("-mrg-management-")) continue;
				
				// skip Red Hat Software Collections
				// rhel-x86_64-workstation-6-rhscl-1
				// rhel-x86_64-workstation-6-rhscl-1-debuginfo
				// rhel-x86_64-server-6-rhscl-1-debuginfo
				// rhel-x86_64-server-6-rhscl-1
				if (rhnChannel.contains("-rhscl-")) continue;
				
				// skip Red Hat Enterprise Virtualization
				//	rhel-x86_64-server-6-rhevm-3-beta
				//  rhel-x86_64-server-6-rhevh
				//	rhel-x86_64-rhev-mgmt-agent-6
				if (rhnChannel.contains("-rhevm-")) continue;
				if (rhnChannel.contains("-rhevh-") || rhnChannel.endsWith("-rhevh")) continue;
				if (rhnChannel.contains("-rhev-mgmt-")) continue;
				
				// skip Red Hat Storage
				//	rhel-x86_64-server-6-rhs-2.0
				if (rhnChannel.contains("-rhs-")) continue;
				
				// skip Red Hat Developer Toolset
				//	rhel-i386-server-dts-5
				//	rhel-i386-server-dts2-5
				if (rhnChannel.contains("-dts-")) continue;
				if (rhnChannel.contains("-dts2-")) continue;	
				
				// skip Red Hat Hardware Certification Test Suite
				//	rhel-x86_64-server-hts-6
				if (rhnChannel.contains("-hts-")) continue;
				
				// skip Red Hat Certification (for RHEL Server)		// productId 282
				//	rhel-i386-server-6-cert-beta
				//	rhel-ppc64-server-6-cert-beta
				//	rhel-s390x-server-6-cert-beta
				//	rhel-x86_64-server-6-cert-beta
				if (rhnChannel.endsWith("-server-6-cert-beta")) continue;
				if (rhnChannel.endsWith("-server-6-cert")) continue;
				
				// skip Red Hat Certification (for RHEL Server)		// productId 282
				//	rhel-i386-server-7-cert-beta
				//	rhel-ppc64-server-7-cert-beta
				//	rhel-s390x-server-7-cert-beta
				//	rhel-x86_64-server-7-cert-beta
				if (rhnChannel.endsWith("-server-7-cert-beta")) continue;
				if (rhnChannel.endsWith("-server-7-cert")) continue;
				
				// skip Red Hat Directory Server
				//	rhel-x86_64-server-5-rhdirserv-8
				if (rhnChannel.contains("-rhdirserv-")) continue;
				
				// skip Red Hat S-JIS Support (for RHEL Server)
				//	rhel-i386-server-sjis-6
				if (rhnChannel.contains("-sjis-")) continue;
				
				// skip Red Hat EUCJP Support (for RHEL Server)
				//	rhel-x86_64-server-eucjp-6
				if (rhnChannel.contains("-eucjp-")) continue;
				
				// skip Red Hat CloudForms
				//	rhel-x86_64-server-6-cf-ce-1
				//	rhel-x86_64-server-6-cf-me-3
				//	rhel-x86_64-server-6-cf-se-1
				if (rhnChannel.contains("-cf-ce-")) continue;
				if (rhnChannel.contains("-cf-me-")) continue;
				if (rhnChannel.contains("-cf-se-")) continue;
				
				// skip Red Hat Open vSwitch
				//	rhel-x86_64-server-6-ovs-supplemental
				if (rhnChannel.contains("-ovs-")) continue;
				
				// skip Red Hat Enterprise Linux High Performance Networking (for RHEL Compute Node)
				// skip Red Hat Enterprise Linux High Performance Networking (for RHEL for IBM POWER)
				// skip Red Hat Enterprise Linux High Performance Networking (for RHEL Server)
				//	rhel-x86_64-hpc-node-hpn-6
				//	rhel-ppc64-server-hpn-6
				//	rhel-x86_64-server-hpn-6
				if (rhnChannel.contains("-hpn-")) continue;
				
				// skip Red Hat Enterprise Linux for SAP
				//	rhel-x86_64-server-sap-6
				if (rhnChannel.contains("-sap-")) continue;
				
				// skip Kernel Derivative Works for Bluegene/Q
				//	rhel-ppc64-server-bluegene-6
				if (rhnChannel.contains("-bluegene-")) continue;
				
				// skip Kernel Derivative Works for HPC for Power Systems
				//	rhel-ppc64-server-p7ih-6
				if (rhnChannel.contains("-p7ih-")) continue;
				
				// skip Red Hat Enterprise Linux Resilient Storage (for RHEL Server)
				//	rhel-ppc-server-cluster-storage-5
				if (rhnChannel.contains("-cluster-storage-")) continue;
				
				// skip Red Hat Storage Server for On-premise	// productId 186
				//	rhel-x86_64-server-6-rh-gluster-3-samba
				if (rhnChannel.contains("-rh-gluster-")) continue;
				
				// skip Red Hat Enterprise Linux High Availability (for RHEL Server)
				//	rhel-ppc-server-cluster-5
				if (rhnChannel.contains("-cluster-")) continue;
				
				// skip Red Hat Certificate System
				//	rhel-x86_64-server-5-rhcmsys-8
				if (rhnChannel.contains("-rhcmsys-")) continue;
				
				// skip all fastrack rpms due to EOL..
				// https://bugzilla.redhat.com/show_bug.cgi?id=1555913#c8
				// https://bugzilla.redhat.com/show_bug.cgi?id=1549766#c4
				// Red Hat Enterprise Linux 7 Server - Fastrack (RPMs)  rhel-7-server-fastrack-rpms
				// Red Hat Enterprise Linux 6 Server - Fastrack (RPMs)  rhel-6-server-fastrack-rpms
				// Red Hat Enterprise Linux 5 Server - Fastrack (RPMs)  rhel-5-server-fastrack-rpms
				if (rhnChannel.contains("-fastrack-")) continue;
			}
			
			// bugzillas
			Set<String> bugIds = new HashSet<String>();
			
			// Bug 1078527 - channel-cert-mapping for ComputeNode rhel-7 product certs are missing and wrong
			if (rhnChannel.equals("rhel-x86_64-hpc-node-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-rh-common-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-7-debuginfo")) {
				bugIds.add("1078527");
			}
			
			// Bug 1105279 - rhn channel rhel-x86_64-server-scalefs-5 maps to a version 5.10 product cert - should be 5.1
			if (rhnChannel.equals("rhel-x86_64-server-scalefs-5") ||
				rhnChannel.equals("rhel-x86_64-server-scalefs-5-beta")) {
				bugIds.add("1105279");
			}
			
			// Bug 1176260 - the RHN RHEL Channels 'rhel-<ARCH>-<VARIANT>-7-thirdparty-oracle-java-beta' map to a '7.0' version cert; should be '7.1 Beta'
			if (rhnChannel.equals("rhel-x86_64-client-7-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-7-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-server-7-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-workstation-7-thirdparty-oracle-java-beta")) {
				if (clienttasks.redhatReleaseXY.equals("7.1")) bugIds.add("1176260");
			}
			
			// Bug 1263432 - the RHN RHEL Channels 'rhel-x86_64-<VARIANT>-7-thirdparty-oracle-java-beta' map to a '7.1' version cert; should be '7.2 Beta'
			if (rhnChannel.equals("rhel-x86_64-client-7-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-7-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-server-7-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-workstation-7-thirdparty-oracle-java-beta")) {
				if (clienttasks.redhatReleaseXY.equals("7.2")) bugIds.add("1263432");
			}
			
			// Bug 1320647 - rhn channels 'rhel-ARCH-workstation-6-thirdparty-oracle-java-beta' should maps to the Beta product cert, not the GA cert.
			if (rhnChannel.equals("rhel-i386-workstation-6-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-workstation-6-thirdparty-oracle-java-beta")) {
				if (clienttasks.redhatReleaseXY.equals("6.8")) bugIds.add("1320647");
			}
			
			// Bug 1349584 - RHN RHEL Channels 'rhel-x86_64-<VARIANT>-7-thirdparty-oracle-java' map to a '7.2' version cert; should be '7.3'
			if (rhnChannel.equals("rhel-x86_64-client-7-thirdparty-oracle-java") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-7-thirdparty-oracle-java") ||
				rhnChannel.equals("rhel-x86_64-server-7-thirdparty-oracle-java") ||
				rhnChannel.equals("rhel-x86_64-workstation-7-thirdparty-oracle-java")) {
				if (clienttasks.redhatReleaseXY.equals("7.3")) bugIds.add("1349584");
			}
			
			// Bug 1349592 - RHN RHEL Channels 'rhel-x86_64-<VARIANT>-7-thirdparty-oracle-java-beta' map to a '7.2' version cert; should be '7.3 Beta'
			if (rhnChannel.equals("rhel-x86_64-client-7-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-7-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-server-7-thirdparty-oracle-java-beta") ||
				rhnChannel.equals("rhel-x86_64-workstation-7-thirdparty-oracle-java-beta")) {
				if (clienttasks.redhatReleaseXY.equals("7.3")) bugIds.add("1349592");
			}
			
			// Bug 1464236 - RHN RHEL Channels 'rhel-x86_64-<VARIANT>-7-thirdparty-oracle-java' map to a '7.3' version cert; should be '7.4'
			if (rhnChannel.equals("rhel-x86_64-client-7-thirdparty-oracle-java") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-7-thirdparty-oracle-java") ||
				rhnChannel.equals("rhel-x86_64-server-7-thirdparty-oracle-java") ||
				rhnChannel.equals("rhel-x86_64-workstation-7-thirdparty-oracle-java")) {
				if (clienttasks.redhatReleaseXY.equals("7.3")) bugIds.add("1464236");
			}
			
			// Bug 1549766 - Numerous RHN RHEL Channels map to a RHEL '7.4' version certificate instead of the latest '7.5' version
			if (rhnChannel.equals("rhel-ppc64-server-extras-7") ||
				rhnChannel.equals("rhel-ppc64-server-extras-7-debuginfo") ||
				rhnChannel.equals("rhel-ppc64-server-fastrack-7") ||
				rhnChannel.equals("rhel-ppc64-server-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-ppc64-server-optional-7") ||
				rhnChannel.equals("rhel-ppc64-server-optional-7-debuginfo") ||
				rhnChannel.equals("rhel-ppc64-server-optional-fastrack-7") ||
				rhnChannel.equals("rhel-ppc64-server-optional-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-ppc64-server-rh-common-7") ||
				rhnChannel.equals("rhel-ppc64-server-rh-common-7-debuginfo") ||
				rhnChannel.equals("rhel-ppc64-server-supplementary-7") ||
				rhnChannel.equals("rhel-ppc64-server-supplementary-7-debuginfo") ||
				rhnChannel.equals("rhel-s390x-server-extras-7") ||
				rhnChannel.equals("rhel-s390x-server-extras-7-debuginfo") ||
				rhnChannel.equals("rhel-s390x-server-fastrack-7") ||
				rhnChannel.equals("rhel-s390x-server-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-s390x-server-optional-7") ||
				rhnChannel.equals("rhel-s390x-server-optional-7-debuginfo") ||
				rhnChannel.equals("rhel-s390x-server-optional-fastrack-7") ||
				rhnChannel.equals("rhel-s390x-server-optional-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-s390x-server-rh-common-7") ||
				rhnChannel.equals("rhel-s390x-server-rh-common-7-debuginfo") ||
				rhnChannel.equals("rhel-s390x-server-supplementary-7") ||
				rhnChannel.equals("rhel-s390x-server-supplementary-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-client-extras-7") ||
				rhnChannel.equals("rhel-x86_64-client-extras-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-client-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-client-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-client-optional-7") ||
				rhnChannel.equals("rhel-x86_64-client-optional-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-client-optional-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-client-optional-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-client-rh-common-7") ||
				rhnChannel.equals("rhel-x86_64-client-rh-common-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-client-supplementary-7") ||
				rhnChannel.equals("rhel-x86_64-client-supplementary-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-extras-7") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-extras-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-optional-7") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-optional-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-optional-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-optional-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-rh-common-7") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-rh-common-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-supplementary-7") ||
				rhnChannel.equals("rhel-x86_64-hpc-node-supplementary-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-server-extras-7") ||
				rhnChannel.equals("rhel-x86_64-server-extras-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-server-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-server-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-server-ha-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-server-ha-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-server-optional-7") ||
				rhnChannel.equals("rhel-x86_64-server-optional-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-server-optional-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-server-optional-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-server-rh-common-7") ||
				rhnChannel.equals("rhel-x86_64-server-rh-common-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-server-rs-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-server-rs-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-server-supplementary-7") ||
				rhnChannel.equals("rhel-x86_64-server-supplementary-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-server-v2vwin-7") ||
				rhnChannel.equals("rhel-x86_64-server-v2vwin-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-workstation-extras-7") ||
				rhnChannel.equals("rhel-x86_64-workstation-extras-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-workstation-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-workstation-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-workstation-optional-7") ||
				rhnChannel.equals("rhel-x86_64-workstation-optional-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-workstation-optional-fastrack-7") ||
				rhnChannel.equals("rhel-x86_64-workstation-optional-fastrack-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-workstation-rh-common-7") ||
				rhnChannel.equals("rhel-x86_64-workstation-rh-common-7-debuginfo") ||
				rhnChannel.equals("rhel-x86_64-workstation-supplementary-7") ||
				rhnChannel.equals("rhel-x86_64-workstation-supplementary-7-debuginfo") ||
				rhnChannel.equals("")) {
				if (clienttasks.redhatReleaseXY.equals("7.5")) bugIds.add("1549766");
			}
			
			// Object bugzilla, String productBaselineRhnChannel, String productBaselineProductId
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[]{blockedByBzBug,	rhnChannel}));
		}
		
		return ll;
	}
	
	
	
	
	
	// Candidates for an automated Test:

	
	
	
	// Configuration methods ***********************************************************************
	

	@BeforeClass(groups="setup")
	public void setupBeforeClass() {
		if (clienttasks==null) return;
		
		// determine the full path to the channelCertMappingFile
		baseProductsDir+="-"+clienttasks.redhatReleaseX;
		channelCertMappingFilename = baseProductsDir+"/"+channelCertMappingFilename;
		
		// make sure needed rpms are installed
		for (String pkg : new String[]{"subscription-manager-migration", "subscription-manager-migration-data", "expect"}) {
			Assert.assertTrue(clienttasks.isPackageInstalled(pkg),"Required package '"+pkg+"' is installed for MigrationTests.");
		}
		
		// configure a valid sslCACert in /etc/sysconfig/rhn/up2date
		setupRhnCACert();
	}
	
	
//	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass"})
//	public void rememberOriginallyInstalledRedHatProductCertsBeforeClass() {
//		
//		// review the currently installed product certs and filter out the ones from test automation (indicated by suffix "_.pem")
//		for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
//			if (!productCertFile.getName().endsWith("_.pem")) {	// The product cert files ending in "_.pem" are not true RedHat products
//				originallyInstalledRedHatProductCerts.add(clienttasks.getProductCertFromProductCertFile(productCertFile));
//			}
//		}
//	}
//	
//	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass"})
//	public void rememberOriginallyConfiguredServerUrlBeforeClass() {
//		if (clienttasks==null) return;
//		
//		originalServerHostname 	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
//		originalServerPort		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
//		originalServerPrefix	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
//	}
//	
//	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass"})
//	public void backupProductCertsBeforeClass() {
//		
//		// determine the original productCertDir value
//		//productCertDirRestore = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
//		originalProductCertDir = clienttasks.productCertDir;
//		
//		log.info("Backing up all the currently installed product certs...");
//		client.runCommandAndWait("mkdir -p "+backupProductCertDir+"; rm -f "+backupProductCertDir+"/*.pem");
//		client.runCommandAndWait("cp "+originalProductCertDir+"/*.pem "+backupProductCertDir);
//	}
//	
//	@BeforeGroups(groups="setup",value={"InstallNumMigrateToRhsmWithInstNumber_Test","InstallNumMigrateToRhsm_Test","RhnMigrateClassicToRhsm_Test"})
//	public void configOriginalRhsmProductCertDir() {
//		if (clienttasks==null) return;
//		
//		//clienttasks.config(false, false, true, new String[]{"rhsm","productcertdir",productCertDirOriginal});
//		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", originalProductCertDir);
//	}
	
	@BeforeClass(groups="setup")
//	@AfterGroups(groups="setup",value={"RhnMigrateClassicToRhsmUsingProxyServer_Test"})
	public void removeProxyServerConfigurations() {
		if (clienttasks==null) return;
		
		// remove proxy settings from rhsm.conf
		// these will actually remove the value from the config file; don't do this
		//clienttasks.config(false, true, false, new String[]{"server","proxy_hostname"});
		//clienttasks.config(false, true, false, new String[]{"server","proxy_user"});
		//clienttasks.config(false, true, false, new String[]{"server","proxy_password"});
		//clienttasks.config(false, true, false, new String[]{"server","proxy_port"});
		clienttasks.config(false, false, true, new String[]{"server","proxy_hostname",""});
		clienttasks.config(false, false, true, new String[]{"server","proxy_user",""});
		clienttasks.config(false, false, true, new String[]{"server","proxy_password",""});
		clienttasks.config(false, false, true, new String[]{"server","proxy_port",""});
		
		// remove proxy settings from up2date
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxy", "0");		// enableProxyAuth[comment]=To use an authenticated proxy or not
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "httpProxy", "");			// httpProxy[comment]=HTTP proxy in host:port format, e.g. squid.redhat.com:3128
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth", "0");	// enableProxyAuth[comment]=To use an authenticated proxy or not
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser", "");			// proxyUser[comment]=The username for an authenticated proxy
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword", "");		// proxyPassword[comment]=The password to use for an authenticated proxy
		// Note: On RHEL7, these three configurations will not have been set in /etc/sysconfig/rhn/up2date because a successful rhnreg_ks will not have occurred
		if (clienttasks.getConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth")==null)	{clienttasks.addConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth", "0");}	else {clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth", "0");}	// enableProxyAuth[comment]=To use an authenticated proxy or not
		if (clienttasks.getConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser")==null)		{clienttasks.addConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser", "");}		else {clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser", "");}		// proxyUser[comment]=The username for an authenticated proxy
		if (clienttasks.getConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword")==null)	{clienttasks.addConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword", "");}	else {clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword", "");}	// proxyPassword[comment]=The password to use for an authenticated proxy
	}
	
	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass"})
	public void copyScriptsToClient() throws IOException {
		// copy the rhn-channels.py script to the client
		File rhnChannelsScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-channels.py");
		if (!rhnChannelsScriptFile.exists()) Assert.fail("Failed to find expected script: "+rhnChannelsScriptFile);
		RemoteFileTasks.putFile(client, rhnChannelsScriptFile.toString(), "/usr/local/bin/", "0755");
		
		// copy the rhn-is-registered.py script to the client
		File rhnIsRegisteredScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-is-registered.py");
		if (!rhnIsRegisteredScriptFile.exists()) Assert.fail("Failed to find expected script: "+rhnIsRegisteredScriptFile);
		RemoteFileTasks.putFile(client, rhnIsRegisteredScriptFile.toString(), "/usr/local/bin/", "0755");
		
		// copy the rhn-delete-systems.py script to the client
		File rhnDeleteSystemsScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-delete-systems.py");
		if (!rhnDeleteSystemsScriptFile.exists()) Assert.fail("Failed to find expected script: "+rhnDeleteSystemsScriptFile);
		RemoteFileTasks.putFile(client, rhnDeleteSystemsScriptFile.toString(), "/usr/local/bin/", "0755");
		
		// copy the rhn-migrate-classic-to-rhsm.tcl script to the client
		File expectScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-migrate-classic-to-rhsm.tcl");
		if (!expectScriptFile.exists()) Assert.fail("Failed to find expected script: "+expectScriptFile);
		RemoteFileTasks.putFile(client, expectScriptFile.toString(), "/usr/local/bin/", "0755");
	}
	
	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass","copyScriptsToClient"})
	public void determineRhnClassicBaseAndAvailableChildChannels() throws IOException {
		if (sm_rhnUsername.equals("")) {log.warning("Skipping determination of the base and available RHN Classic channels"); return;}
		if (sm_rhnPassword.equals("")) {log.warning("Skipping determination of the base and available RHN Classic channels"); return;}
		if (sm_rhnHostname.equals("")) {log.warning("Skipping determination of the base and available RHN Classic channels"); return;}

		// get the base channel
		clienttasks.registerToRhnClassic_(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		List<String> rhnChannels = clienttasks.getCurrentRhnClassicChannels();
		//Assert.assertEquals(rhnChannels.size(), 1, "The number of base RHN Classic base channels this system is consuming.");
		if (rhnChannels.isEmpty()) {
			log.warning("When no RHN channels are available to this classically registered system, no product certs will be migrated to RHSM.");
			return; 
		}
		rhnBaseChannel = clienttasks.getCurrentRhnClassicChannels().get(0);

		// get all of the available RHN Classic child channels available for consumption under this base channel
		rhnAvailableChildChannels.clear();
		String serverUrl = sm_rhnHostname; if (!serverUrl.startsWith("http")) serverUrl="https://"+sm_rhnHostname;
		String command = String.format("rhn-channels.py --username=%s --password=%s --serverurl=%s --basechannel=%s --no-custom --available", sm_rhnUsername, sm_rhnPassword, serverUrl, rhnBaseChannel);
///*debugTesting*/ if (true) command = "echo rhel-x86_64-server-5 && echo rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5 && echo rhx-amanda-enterprise-backup-2.6-rhel-x86_64-server-5";
		
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0));
		rhnChannels = new ArrayList<String>();
		if (!result.getStdout().trim().equals("")) {
			rhnChannels	= Arrays.asList(result.getStdout().trim().split("\\n"));
		}
		for (String rhnChannel : rhnChannels) {
			if (!rhnChannel.equals(rhnBaseChannel)) rhnAvailableChildChannels.add(rhnChannel.trim()); 
		}
		Assert.assertTrue(rhnAvailableChildChannels.size()>0,"A positive number of child channels under the RHN Classic base channel '"+rhnBaseChannel+"' are available for consumption.");
	}
	
	@AfterClass(groups={"setup"},alwaysRun=true)
	public void removeRHNSystemIdFileAfterClass() {
		if (clienttasks!=null) {
			clienttasks.removeRhnSystemIdFile();
		}
	}
	
	@BeforeClass(groups={"setup"})
	public void determineCdnProductBaselineMapsBeforeClass() throws IOException, JSONException {

		// Reference: https://engineering.redhat.com/trac/rcm/wiki/Projects/CDNBaseline

		// THE JSON LOOKS LIKE THIS...
		//	[
		//		{
		//			"Content Sets": [
		//				{
		//					"Label": "rhel-hpn-for-rhel-6-server-source-rpms", 
		//					"Repos": [
		//						{
		//							"Relative URL": "/content/dist/rhel/server/6/6.1/i386/hpn/source/SRPMS"
		//						}, 
		//						{
		//							"Relative URL": "/content/dist/rhel/server/6/6.2/x86_64/hpn/source/SRPMS"
		//						}
		//					]
		//				}
		//			], 
		//			"Name": "Red Hat Enterprise Linux High Performance Networking (for RHEL Server)", 
		//			"Product ID": "132", 
		//			"RHN Channels": [
		//				"rhel-x86_64-server-hpn-6", 
		//				"rhel-x86_64-server-hpn-6-beta-debuginfo", 
		//				"rhel-x86_64-server-hpn-6-beta", 
		//				"rhel-x86_64-server-hpn-6-debuginfo"
		//			]
		//		}
		//	]
		client.runCommandAndWaitWithoutLogging("cat "+clienttasks.rhnDefinitionsDir+sm_rhnDefinitionsProductBaselineFile);
		JSONArray jsonProducts = new JSONArray(client.getStdout());	
		for (int p = 0; p < jsonProducts.length(); p++) {
			JSONObject jsonProduct = (JSONObject) jsonProducts.get(p);
			String productName = jsonProduct.getString("Name");
			String productId = jsonProduct.getString("Product ID");
			JSONArray jsonRhnChannels = jsonProduct.getJSONArray("RHN Channels");
			
			// process each of the RHN Channels
			for (int r=0; r<jsonRhnChannels.length(); r++) {
				String rhnChannel = jsonRhnChannels.getString(r);
				
				// store the rhnChannel in the cdnProductBaselineChannelMap
				if (cdnProductBaselineChannelMap.containsKey(rhnChannel)) {
					if (!cdnProductBaselineChannelMap.get(rhnChannel).contains(productId)) {
						cdnProductBaselineChannelMap.get(rhnChannel).add(productId);
					}
				} else {
					List<String> productIds = new ArrayList<String>(); productIds.add(productId);
					cdnProductBaselineChannelMap.put(rhnChannel, productIds);
				}
				
				// also store the inverse of this map into cdnProductBaselineProductIdMap
				if (cdnProductBaselineProductIdMap.containsKey(productId)) {
					if (!cdnProductBaselineProductIdMap.get(productId).contains(rhnChannel)) {
						cdnProductBaselineProductIdMap.get(productId).add(rhnChannel);
					}
				} else {
					List<String> rhnChannels = new ArrayList<String>(); rhnChannels.add(rhnChannel);
					cdnProductBaselineProductIdMap.put(productId, rhnChannels);
				}
			}
		}
	}
	
	@BeforeClass(groups={"setup"})
	public void determineCdnProductCertsMapsBeforeClass() throws IOException, JSONException {

		// Reference: http://git.app.eng.bos.redhat.com/?p=rcm/rcm-metadata.git;a=blob;f=cdn/product-certs.json

		// THE JSON LOOKS LIKE THIS...
		//	{
		//		"rhel-i386-rhev-agent-6-server": {
		//	         "Product Cert CN": "Red Hat Product ID [625d9640-910d-4e56-8fc3-d98163bd81a0]", 
		//	         "Product Cert file": "/rhel-6.3/Server-Server-i386-d98163bd81a0-69.pem", 
		//	         "Product ID": "69"
		//	     }, 
		//	     "rhel-i386-rhev-agent-6-workstation": {
		//	         "Product Cert CN": "Red Hat Product ID [b29e876e-746f-4062-8977-a0bad2ffd9b4]", 
		//	         "Product Cert file": "/rhel-6.3/Workstation-Workstation-i386-a0bad2ffd9b4-71.pem", 
		//	         "Product ID": "71"
		//	     }, 
		//	     "rhel-i386-server-5": {
		//	         "Product Cert CN": "Red Hat Product ID [693973c8-9bb8-4b01-b27d-2445a981e321]", 
		//	         "Product Cert file": "/rhel-5.8/Server-Server-i386-2445a981e321-69.pem", 
		//	         "Product ID": "69"
		//	     }, 
		//	}
		client.runCommandAndWaitWithoutLogging("cat "+clienttasks.rhnDefinitionsDir+sm_rhnDefinitionsProductCertsFile);
		JSONObject jsonChannelCertsMap = new JSONObject(client.getStdout());
		
		Iterator<String> rhnChannels = jsonChannelCertsMap.keys();
		while (rhnChannels.hasNext()) {
			String rhnChannel = rhnChannels.next();
			JSONObject jsonChannelCertMap = jsonChannelCertsMap.getJSONObject(rhnChannel);
			String productCertFile = jsonChannelCertMap.getString("Product Cert file");
			cdnProductCertsChannelMap.put(rhnChannel, new File(productCertFile));
			
//			cdnProductCertsChannelToProductCertMap.put(rhnChannel, clienttasks.getProductCertFromProductCertFile(new File (clienttasks.rhnDefinitionsDir+"/product_ids"+productCertFile)));
		}
	}
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 955824 - Product certs should not be generated with a tag value of "None"
	
	
	
	
	// Protected methods ***********************************************************************
	protected String baseProductsDir = "/usr/share/rhsm/product/RHEL";
	protected String channelCertMappingFilename = "channel-cert-mapping.txt";
	protected List<String> mappedProductCertFilenames = new ArrayList<String>();	// list of all the mapped product cert file names in the mapping file (e.g. Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem)
	protected Map<String,String> channelsToProductCertFilenamesMap = new HashMap<String,String>();	// map of all the channels to product cert file names (e.g. key=rhn-tools-rhel-x86_64-server-5 value=Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem)
	protected Map<String,List<String>> cdnProductBaselineChannelMap = new HashMap<String,List<String>>();	// map of all the channels to list of productIds (e.g. key=rhn-tools-rhel-x86_64-server-5 value=[69,169,269])
	protected Map<String,File> cdnProductCertsChannelMap = new HashMap<String,File>();	// map generated from cdn/product-certs.json of all the channels to product cert files (e.g. key=jb-ewp-5-i386-server-5-rpm value=/jbewp-5.0/Server-JBEWP-i386-bca12d9b039b-184.pem)
//	protected Map<String,ProductCert> cdnProductCertsChannelToProductCertMap = new HashMap<String,ProductCert>();	// map generated from cdn/product-certs.json of all the channels to product cert files (e.g. key=jb-ewp-5-i386-server-5-rpm value=/jbewp-5.0/Server-JBEWP-i386-bca12d9b039b-184.pem)
	protected Map<String,List<String>> cdnProductBaselineProductIdMap = new HashMap<String,List<String>>();	// map of all the productIds to list of channels (e.g. key=69 value=[rhn-tools-rhel-x86_64-server-5, rhn-tools-rhel-x86_64-server-5-debug-info])	// inverse of cdnProductBaselineChannelMap
//	protected List<ProductCert> originallyInstalledRedHatProductCerts = new ArrayList<ProductCert>();
//	protected String migrationFromFact				= "migration.migrated_from";
//	protected String migrationSystemIdFact			= "migration.classic_system_id";
//	protected String migrationDateFact				= "migration.migration_date";
//	protected String originalProductCertDir			= null;
//	protected String backupProductCertDir			= "/tmp/backupOfProductCertDir";
//	protected String nonDefaultProductCertDir		= "/tmp/migratedProductCertDir";
//	protected String machineInstNumberFile			= "/etc/sysconfig/rhn/install-num";
//	protected String backupMachineInstNumberFile	= machineInstNumberFile+".bak";
	protected String rhnBaseChannel = null;
	protected List<String> rhnAvailableChildChannels = new ArrayList<String>();
//	static public String installNumTool = "install-num-migrate-to-rhsm";
//	static public String rhnMigrateTool = "rhn-migrate-classic-to-rhsm";
//	protected String originalServerHostname;
//	protected String originalServerPort;
//	protected String originalServerPrefix;
	
	
	
	/**
	 * Extract the suffix pem filename from the long mapped filename.
	 * @param productCertFilename example: Server-ClusterStorage-ppc-a3fea9e1dde3-90.pem
	 * @return example: 90.pem
	 */
	public static String getPemFileNameFromProductCertFilename(String productCertFilename) {
		// Server-ClusterStorage-ppc-a3fea9e1dde3-90.pem
		return productCertFilename.split("-")[productCertFilename.split("-").length-1];
	}
	
	/**
	 * Extract the productId from the long mapped filename.
	 * @param productCertFilename example: Server-ClusterStorage-ppc-a3fea9e1dde3-90.pem
	 * @return example: 90
	 */
	public static String getProductIdFromProductCertFilename(String productCertFilename) {
		// Server-ClusterStorage-ppc-a3fea9e1dde3-90.pem
		String pemFilename = getPemFileNameFromProductCertFilename(productCertFilename);
		return pemFilename.replace(".pem", "");
	}
	
	
	
	// Data Providers ***********************************************************************
	
	
	
	@DataProvider(name="RhnChannelFromProductBaselineData")
	public Object[][] getRhnChannelFromProductBaselineDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnChannelFromProductBaselineDataAsListOfLists());
	}
	public List<List<Object>> getRhnChannelFromProductBaselineDataAsListOfLists() throws JSONException {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
				
		for (String productId : cdnProductBaselineProductIdMap.keySet()) {
			for (String rhnChannel : cdnProductBaselineProductIdMap.get(productId)) {

				// filter out all RHN Channels not associated with this release  (e.g., assume that an rhn channel containing "-5-" or ends in "-5" is only applicable to rhel5 
				if (!(rhnChannel.contains("-"+clienttasks.redhatReleaseX+"-") || rhnChannel.endsWith("-"+clienttasks.redhatReleaseX))) continue;
				
				// skip on these RHN Channels that slip through this ^ filter
				// [root@jsefler-onprem-5server tmp]# grep jboss /tmp/product-baseline.json | grep -v Label
	            // "rhel-x86_64-server-6-rhevm-3-jboss-5", 
	            // "rhel-x86_64-server-6-rhevm-3-jboss-5-beta", 
	            // "rhel-x86_64-server-6-rhevm-3-jboss-5-debuginfo", 
	            // "rhel-x86_64-server-6-rhevm-3-jboss-5-beta-debuginfo"
				List<String> rhnChannelExceptions = Arrays.asList("rhel-x86_64-server-6-rhevm-3-jboss-5","rhel-x86_64-server-6-rhevm-3-jboss-5-beta","rhel-x86_64-server-6-rhevm-3-jboss-5-debuginfo","rhel-x86_64-server-6-rhevm-3-jboss-5-beta-debuginfo");
				if (rhnChannelExceptions.contains(rhnChannel) && !clienttasks.redhatReleaseX.equals(/*"5"*/"6")) continue;
				
				// bugzillas
				Set<String> bugIds = new HashSet<String>();
				if (rhnChannel.contains("-rhev-agent-") && clienttasks.redhatReleaseX.equals("5")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 786278 - RHN Channels for -rhev- and -vt- in the channel-cert-mapping.txt are not mapped to a productId
					bugIds.add("786278");
				}
				if (rhnChannel.contains("-vt-")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 786278 - RHN Channels for -rhev- and -vt- in the channel-cert-mapping.txt are not mapped to a productId
					bugIds.add("786278");
				}
				if (rhnChannel.startsWith("rhel-i386-rhev-agent-") /* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 816364 - channel-cert-mapping.txt is missing a mapping for product 150 "Red Hat Enterprise Virtualization" on i386
					bugIds.add("816364");
				}
				if (rhnChannel.endsWith("-beta") && clienttasks.redhatReleaseX.equals("5")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 786203 - all RHN *beta Channels in channel-cert-mapping.txt are mapped to "none" instead of valid productId
					bugIds.add("786203");
				}			
				if (rhnChannel.endsWith("-debuginfo") && clienttasks.redhatReleaseX.equals("5")) { 
					// Bug 786140 - RHN Channels for "*debuginfo" are missing from the channel-cert-mapping.txt 
					bugIds.add("786140");
				}
				if (rhnChannel.startsWith("rhel-x86_64-server-6-rhevh") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3-jboss-5") ||
					rhnChannel.startsWith("rhel-x86_64-server-sjis-6") ||
					rhnChannel.startsWith("rhel-x86_64-server-sap-6") ||
					/*
					rhnChannel.startsWith("rhel-x86_64-server-optional-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-sfs-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-ha-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-rs-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-lb-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-workstation-sfs-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-workstation-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-workstation-optional-6-htb") ||
					*/
					rhnChannel.startsWith("rhel-x86_64-rhev-mgmt-agent-6") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-tools-1") || rhnChannel.startsWith("rhel-i386-server-6-cf-tools-1") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-ae-1") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-ce-1") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-se-1") ||	
					rhnChannel.startsWith("sam-rhel-x86_64-server-6-htb") || rhnChannel.startsWith("sam-rhel-x86_64-server-6-beta")) { 
					// Bug 799152 - subscription-manager-migration-data is missing some product certs for RHN Channels in product-baseline.json
					bugIds.add("799152");
				}
				if (rhnChannel.equals("rhel-s390x-server-6") ||
					rhnChannel.equals("rhel-s390x-server-optional-6") ||
					rhnChannel.equals("rhel-s390x-server-supplementary-6")) { 
					// Bug 799103 - no mapping for s390x product cert included in the subscription-manager-migration-data
					bugIds.add("799103");
				}
				if (rhnChannel.equals("sam-rhel-x86_64-server-6") ||
					rhnChannel.equals("sam-rhel-x86_64-server-6-debuginfo")) { 
					// Bug 815433 - sam-rhel-x86_64-server-6-beta channel mapping needs replacement in channel-cert-mapping.txt 
					bugIds.add("815433");
				}
				if (productId.equals("167")) {
					// Bug 811633 - channel-cert-mapping.txt is missing a mapping for product 167 "Red Hat CloudForms"
					bugIds.add("811633");
				}
				if (productId.equals("183") || productId.equals("184") || productId.equals("185")) if (clienttasks.redhatReleaseX.equals("6")) {
					// Bug 825603 - channel-cert-mapping.txt is missing a mapping for JBoss product ids 183,184,185
					bugIds.add("825603");
				}
				if (rhnChannel.contains("-dts-")) if (clienttasks.redhatReleaseX.equals("6")) { 
					// Bug 820749 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
					bugIds.add("820749");
				}
				if (rhnChannel.contains("-dts-")) if (clienttasks.redhatReleaseX.equals("5")) { 
					// Bug 852551 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
					bugIds.add("852551");
				}
				if (productId.equals("195")) if (clienttasks.redhatReleaseX.equals("5")) {
					// Bug 869008 - mapping for productId 195 "Red Hat Developer Toolset (for RHEL for IBM POWER)" is missing
					bugIds.add("869008");
				}
				if (productId.equals("195")) if (clienttasks.redhatReleaseX.equals("6")) {
					// Bug 875802 - mapping for productId 195 "Red Hat Developer Toolset (for RHEL for IBM POWER)" is missing
					bugIds.add("875802");
				}
				if (productId.equals("181")) {
					// Bug 840148 - missing product cert corresponding to "Red Hat EUCJP Support (for RHEL Server)"
					bugIds.add("840148");
					// Bug 847069 - Add certificates for rhel-x86_64-server-eucjp-5* channels.
					bugIds.add("847069");
				}
				if (rhnChannel.startsWith("rhel-i386-rhev-agent-5-")) { 
					// Bug 849305 - rhel-i386-rhev-agent-5-* maps in channel-cert-mapping.txt do not match CDN Product Baseline
					bugIds.add("849305");
				}
				if (rhnChannel.startsWith("jbappplatform-4.2-els-")) { 
					// Bug 861470 - JBoss Enterprise Application Platform - ELS (jbappplatform-4.2.0) 192.pem product certs are missing from subscription-manager-migration-data
					bugIds.add("861470");
				}
				if (rhnChannel.startsWith("rhel-x86_64-rhev-mgmt-agent-5")) { 
					// Bug 861420 - Red Hat Enterprise Virtualization (rhev-3.0) 150.pem product certs are missing from subscription-manager-migration-data
					bugIds.add("861420");
				}
				if (rhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-5-debuginfo") || rhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-5-beta-debuginfo")) { 
					// Bug 865566 - RHEL-5/channel-cert-mapping.txt is missing a mapping for two rhev debuginfo channels
					bugIds.add("865566");
				}
				if (productId.equals("167") || productId.equals("155") || productId.equals("186") || productId.equals("191") || productId.equals("188") || productId.equals("172")) if (clienttasks.redhatReleaseX.equals("6")) {
					// Bug 872959 - many product certs and their RHN Channel mappings are missing from the RHEL64 subscription-manager-migration-data
					bugIds.add("872959");
				}
				if (productId.equals("197") || productId.equals("198")) {
					// Bug 875760 - some openshift product certs and their RHN Channel mappings are missing from the RHEL64 subscription-manager-migration-data
					bugIds.add("875760");
				}
				if (rhnChannel.startsWith("rhel-x86_64-server-6-ost-folsom")) { 
					// Bug 884657 - the server-6-ost-folsom channels need to be mapped into channel-cert-mapping.txt
					bugIds.add("884657");
				}
				if (rhnChannel.equals("rhel-x86_64-hpc-node-dts-6") || rhnChannel.equals("rhel-x86_64-hpc-node-dts-6-debuginfo")) {
					// Bug 820749 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
					bugIds.add("820749");
					// Bug 884688 - RHN channel "rhel-x86_64-hpc-node-dts-6" is mapped to 177, but the product cert 177.pem is missing 
					bugIds.add("884688");
				}
				if (rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3.1")) { 
					// Bug 888791 - product cert mappings for RHN Channels rhel-x86_64-server-6-rhevm-3.1* are missing
					bugIds.add("888791");
				}
				if (rhnChannel.startsWith("rhel-i386-server-sjis-6")) {	// rhel-i386-server-sjis-6 rhel-i386-server-sjis-6-debuginfo rhel-i386-server-sjis-6-beta rhel-i386-server-sjis-6-beta-debuginfo
					// Bug 896195 - rhel-i386-server-sjis-6 channels are not yet mapped in channel-cert-mapping.txt
					bugIds.add("896195");
				}
				if (rhnChannel.contains("-dts-5-beta")) {	// rhel-i386-server-dts-5-beta rhel-i386-server-dts-5-beta-debuginfo rhel-x86_64-server-dts-5-beta rhel-x86_64-server-dts-5-beta-debuginfo
					// Bug 966683 - the dts beta channels should be mapped to the RHB product cert 180
					bugIds.add("966683");
				}
				if (rhnChannel.contains("-rhev-mgmt-agent-5")) {	// rhel-x86_64-rhev-mgmt-agent-5 rhel-x86_64-rhev-mgmt-agent-5-beta
					// Bug 966696 - Red Hat Enterprise Virtualization (rhev-3.0) 150.pem product certs are missing from subscription-manager-migration-data 
					bugIds.add("966696");
				}
				
				// Object bugzilla, String productBaselineRhnChannel, String productBaselineProductId
				BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				ll.add(Arrays.asList(new Object[]{blockedByBzBug,	rhnChannel,	productId}));
			}
		}
		
		return ll;
	}
	
	
	@DataProvider(name="RhnChannelFromProductCertsData")
	public Object[][] getRhnChannelFromProductCertsDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnChannelFromProductCertsDataAsListOfLists());
	}
	public List<List<Object>> getRhnChannelFromProductCertsDataAsListOfLists() throws JSONException {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
			for (String rhnChannel : cdnProductCertsChannelMap.keySet()) {
///*debugTesting*/ if (!rhnChannel.equals("rhel-x86_64-server-sap-7")) continue;
///*debugTesting*/ if (!rhnChannel.equals("rhel-x86_64-server-rs-7")) continue;
///*debugTesting*/ if (!rhnChannel.equals("rhel-x86_64-server-ha-7")) continue;
				File productCertFile = cdnProductCertsChannelMap.get(rhnChannel);
				String productId = getProductIdFromProductCertFilename(productCertFile.getPath());
				
				// filter out all RHN Channels that map to a release of rhel that does not equal the current release (clienttasks.redhatReleaseXY)
				String regex="/rhel-(\\d+)\\.(\\d+).*";	// match it against example like: /rhel-5.9-beta/Server-Server-x86_64-4b918bda53c0-69.pem  /rhel-6.3/EUS-HighAvailability-x86_64-51676442768e-84.pem  /mrg-2.1/Server-MRG-R-x86_64-e1d154eaac1f-172.pem
				Pattern pattern = Pattern.compile(regex/*,Pattern.DOTALL, Pattern.MULTILINE*/);
				Matcher matcher = pattern.matcher(productCertFile.getPath());
				if (matcher.find()) {
					if (!clienttasks.redhatReleaseXY.equals(matcher.group(1)+"."+matcher.group(2))) {
						log.fine("Skipping rhnChannel '"+rhnChannel+"' mapping to '"+productCertFile.getPath()+"' because it does not apply to this release of RHEL"+clienttasks.redhatReleaseXY);
						continue;
					}
				}
								
				// bugzillas
				Set<String> bugIds = new HashSet<String>();
				if (rhnChannel.contains("-rhev-agent-") && clienttasks.redhatReleaseX.equals("5")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 786278 - RHN Channels for -rhev- and -vt- in the channel-cert-mapping.txt are not mapped to a productId
					bugIds.add("786278");
				}
				if (rhnChannel.contains("-vt-")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 786278 - RHN Channels for -rhev- and -vt- in the channel-cert-mapping.txt are not mapped to a productId
					bugIds.add("786278");
				}
				if (rhnChannel.startsWith("rhel-i386-rhev-agent-") /* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 816364 - channel-cert-mapping.txt is missing a mapping for product 150 "Red Hat Enterprise Virtualization" on i386
					bugIds.add("816364");
				}
				if (rhnChannel.endsWith("-beta") && clienttasks.redhatReleaseX.equals("5")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 786203 - all RHN *beta Channels in channel-cert-mapping.txt are mapped to "none" instead of valid productId
					bugIds.add("786203");
				}			
				if (rhnChannel.endsWith("-debuginfo") && clienttasks.redhatReleaseX.equals("5")) { 
					// Bug 786140 - RHN Channels for "*debuginfo" are missing from the channel-cert-mapping.txt 
					bugIds.add("786140");
				}
				if (rhnChannel.startsWith("rhel-x86_64-server-6-rhevh") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3-jboss-5") ||
					rhnChannel.startsWith("rhel-x86_64-server-sjis-6") ||
					rhnChannel.startsWith("rhel-x86_64-server-sap-6") ||
					/*
					rhnChannel.startsWith("rhel-x86_64-server-optional-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-sfs-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-ha-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-rs-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-lb-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-workstation-sfs-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-workstation-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-workstation-optional-6-htb") ||
					*/
					rhnChannel.startsWith("rhel-x86_64-rhev-mgmt-agent-6") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-tools-1") || rhnChannel.startsWith("rhel-i386-server-6-cf-tools-1") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-ae-1") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-ce-1") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-se-1") ||	
					rhnChannel.startsWith("sam-rhel-x86_64-server-6-htb") || rhnChannel.startsWith("sam-rhel-x86_64-server-6-beta")) { 
					// Bug 799152 - subscription-manager-migration-data is missing some product certs for RHN Channels in product-baseline.json
					bugIds.add("799152");
				}
				if (rhnChannel.equals("rhel-s390x-server-6") ||
					rhnChannel.equals("rhel-s390x-server-optional-6") ||
					rhnChannel.equals("rhel-s390x-server-supplementary-6")) { 
					// Bug 799103 - no mapping for s390x product cert included in the subscription-manager-migration-data
					bugIds.add("799103");
				}
				if (rhnChannel.equals("sam-rhel-x86_64-server-6") ||
					rhnChannel.equals("sam-rhel-x86_64-server-6-debuginfo")) { 
					// Bug 815433 - sam-rhel-x86_64-server-6-beta channel mapping needs replacement in channel-cert-mapping.txt 
					bugIds.add("815433");
				}
				if (productId.equals("167")) {
					// Bug 811633 - channel-cert-mapping.txt is missing a mapping for product 167 "Red Hat CloudForms"
					bugIds.add("811633");
				}
				if (productId.equals("183") || productId.equals("184") || productId.equals("185")) if (clienttasks.redhatReleaseX.equals("6")) {
					// Bug 825603 - channel-cert-mapping.txt is missing a mapping for JBoss product ids 183,184,185
					bugIds.add("825603");
				}
				if (rhnChannel.contains("-dts-")) if (clienttasks.redhatReleaseX.equals("6")) { 
					// Bug 820749 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
					//TODO UNCOMMENT AFTER BUG 884688 IS FIXED bugIds.add("820749");
				}
				if (rhnChannel.contains("-dts-")) if (clienttasks.redhatReleaseX.equals("5")) { 
					// Bug 852551 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
					bugIds.add("852551");
				}
				if (productId.equals("195")) if (clienttasks.redhatReleaseX.equals("5")) {
					// Bug 869008 - mapping for productId 195 "Red Hat Developer Toolset (for RHEL for IBM POWER)" is missing
					bugIds.add("869008");
				}
				if (productId.equals("195")) if (clienttasks.redhatReleaseX.equals("6")) {
					// Bug 875802 - mapping for productId 195 "Red Hat Developer Toolset (for RHEL for IBM POWER)" is missing
					bugIds.add("875802");
				}
				if (productId.equals("181")) {
					// Bug 840148 - missing product cert corresponding to "Red Hat EUCJP Support (for RHEL Server)"
					bugIds.add("840148");
					// Bug 847069 - Add certificates for rhel-x86_64-server-eucjp-5* channels.
					bugIds.add("847069");
				}
				if (rhnChannel.startsWith("rhel-i386-rhev-agent-5-")) { 
					// Bug 849305 - rhel-i386-rhev-agent-5-* maps in channel-cert-mapping.txt do not match CDN Product Baseline
					bugIds.add("849305");
				}
				if (rhnChannel.startsWith("jbappplatform-4.2-els-")) { 
					// Bug 861470 - JBoss Enterprise Application Platform - ELS (jbappplatform-4.2.0) 192.pem product certs are missing from subscription-manager-migration-data
					bugIds.add("861470");
				}
				if (rhnChannel.startsWith("rhel-x86_64-rhev-mgmt-agent-5")) { 
					// Bug 861420 - Red Hat Enterprise Virtualization (rhev-3.0) 150.pem product certs are missing from subscription-manager-migration-data
					bugIds.add("861420");
				}
				if (rhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-5-debuginfo") || rhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-5-beta-debuginfo")) { 
					// Bug 865566 - RHEL-5/channel-cert-mapping.txt is missing a mapping for two rhev debuginfo channels
					bugIds.add("865566");
				}
				if (productId.equals("167") || productId.equals("155") || productId.equals("186") || productId.equals("191") || productId.equals("188") || productId.equals("172")) if (clienttasks.redhatReleaseX.equals("6")) {
					// Bug 872959 - many product certs and their RHN Channel mappings are missing from the RHEL64 subscription-manager-migration-data
					bugIds.add("872959");
				}
				if (productId.equals("197") || productId.equals("198")) {
					// Bug 875760 - some openshift product certs and their RHN Channel mappings are missing from the RHEL64 subscription-manager-migration-data
					bugIds.add("875760");
				}
				if (rhnChannel.startsWith("rhel-x86_64-server-6-ost-folsom")) { 
					// Bug 884657 - the server-6-ost-folsom channels need to be mapped into channel-cert-mapping.txt
					bugIds.add("884657");
				}
				if (rhnChannel.equals("rhel-x86_64-hpc-node-dts-6") || rhnChannel.equals("rhel-x86_64-hpc-node-dts-6-debuginfo")) {
					// Bug 820749 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
					bugIds.add("820749");
					// Bug 884688 - RHN channel "rhel-x86_64-hpc-node-dts-6" is mapped to 177, but the product cert 177.pem is missing 
					bugIds.add("884688");
				}
				if (rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3.1")) { 
					// Bug 888791 - product cert mappings for RHN Channels rhel-x86_64-server-6-rhevm-3.1* are missing
					bugIds.add("888791");
				}
				if (rhnChannel.startsWith("rhel-i386-server-sjis-6")) {	// rhel-i386-server-sjis-6 rhel-i386-server-sjis-6-debuginfo rhel-i386-server-sjis-6-beta rhel-i386-server-sjis-6-beta-debuginfo
					// Bug 896195 - rhel-i386-server-sjis-6 channels are not yet mapped in channel-cert-mapping.txt
					bugIds.add("896195");
				}
				
				if (rhnChannel.startsWith("rhel-x86_64-server-7-ost-6")) {	// rhel-x86_64-server-7-ost-6 rhel-x86_64-server-7-ost-6-debuginfo rhel-x86_64-server-7-ost-6-installer rhel-x86_64-server-7-ost-6-installer-debuginfo
					// Bug 1184653 - RHN channel to product cert mappings for OpenStack-6.0 191.pem are missing from subscription-manager-migration-data
					bugIds.add("1184653");
				}
				
				if (rhnChannel.startsWith("redhat-rhn-satellite-5.7-server")) {	// redhat-rhn-satellite-5.7-server-x86_64-6 redhat-rhn-satellite-5.7-server-s390x-6
					// Bug 1184657 - RHN channel to product cert mappings for Satellite Server 5.7 250.pem are missing from subscription-manager-migration-data
					bugIds.add("1184657");
				}
				
				// Object bugzilla, String productBaselineRhnChannel, String productBaselineProductId
				BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				ll.add(Arrays.asList(new Object[]{blockedByBzBug,	rhnChannel,	productCertFile}));
			}

		
		return ll;
	}
	
	
	@DataProvider(name="getRhnClassicBaseAndAvailableChildChannelsData")
	public Object[][] getRhnClassicBaseAndAvailableChildChannelsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnClassicBaseAndAvailableChildChannelsDataAsListOfLists());
	}
	protected List<List<Object>> getRhnClassicBaseAndAvailableChildChannelsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		
		// add the base channel
		if (rhnBaseChannel!=null) {
			
			// bugzillas
			Set<String> bugIds = new HashSet<String>();
			if (rhnBaseChannel.equals("rhel-x86_64-hpc-node-7")) {
				// Bug 1078527 - channel-cert-mapping for ComputeNode rhel-7 product certs are missing and wrong
				bugIds.add("1078527");
			}
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[]{blockedByBzBug,	rhnBaseChannel}));
		}
		
		// add the child channels
		for (String rhnAvailableChildChannel : rhnAvailableChildChannels) {
			
			// bugzillas
			Set<String> bugIds = new HashSet<String>();
			if (rhnAvailableChildChannel.matches("sam-rhel-.+-server-6-beta.*")) {	// sam-rhel-x86_64-server-6-beta-debuginfo
				// Bug 819092 - channels for sam-rhel-<ARCH>-server-6-beta-* are not yet mapped to product certs in rcm/rhn-definitions.git
				bugIds.add("819092");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-rhui-2(-.*|$)")) {	// rhel-x86_64-server-6-rhui-2 rhel-x86_64-server-6-rhui-2-debuginfo
				// Bug 819089 - channels for rhel-<ARCH>-rhui-2-* are not yet mapped to product certs in rcm/rhn-definitions.git
				bugIds.add("819089");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-6-mrg-.+")) {	// rhel-x86_64-server-6-mrg-grid-execute-2-debuginfo rhel-x86_64-server-6-mrg-messaging-2-debuginfo
				// Bug 819088 - channels for rhel-<ARCH>-server-6-mrg-* are not yet mapped to product certs in rcm/rhn-definitions.git 
				bugIds.add("819088");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-hpc-node-6-mrg-.*")) {	// rhel-x86_64-hpc-node-6-mrg-grid-execute-2  rhel-x86_64-hpc-node-6-mrg-grid-execute-2-debuginfo  rhel-x86_64-hpc-node-6-mrg-management-2  rhel-x86_64-hpc-node-6-mrg-management-2-debuginfo
				// Bug 825608 - channels for rhel-<ARCH>-hpc-node-6-mrg-* are not yet mapped to product certs in rcm/rhn-definitions.git
				bugIds.add("825608");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-v2vwin-6(-.*|$)")) {	// rhel-x86_64-server-v2vwin-6-beta-debuginfo
				// Bug 817791 - v2vwin content does not exist in CDN
				bugIds.add("817791");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-fastrack-6(-.*|$)")) {	// rhel-x86_64-server-ha-fastrack-6-debuginfo
				// Bug 818202 - Using subscription-manager, some repositories like fastrack are not available as they are in rhn.
				bugIds.add("818202");	// CLOSED WONTFIX
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-eucjp-6(-.+|$)")) {	// rhel-x86_64-server-eucjp-6 rhel-x86_64-server-eucjp-6-beta etc.
				// Bug 840148 - missing product cert corresponding to "Red Hat EUCJP Support (for RHEL Server)"
				bugIds.add("840148");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-fastrack-5(-.*|$)")) {	// rhel-x86_64-server-fastrack-5 rhel-x86_64-server-fastrack-5-debuginfo
				// Bug 818202 - Using subscription-manager, some repositories like fastrack are not available as they are in rhn.
				bugIds.add("818202");	// CLOSED WONTFIX
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-5-cf-tools-1(-beta)?-debuginfo")) {	// rhel-x86_64-server-5-cf-tools-1-beta-debuginfo, rhel-x86_64-server-5-cf-tools-1-debuginfo
				// Bug 840099 - debug info channels for rhel-x86_64-server-5-cf-tools are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");	// CLOSED WONTFIX
				// Bug 1105331 - RHN channel to product cert mappings are missing for rhel-x86_64-server-5-cf-tools-1-[beta-]debug channels
				bugIds.add("1105331");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-5-mrg-.*")) {	// rhel-x86_64-server-5-mrg-grid-1 rhel-x86_64-server-5-mrg-grid-1-beta rhel-x86_64-server-5-mrg-grid-2 rhel-x86_64-server-5-mrg-grid-execute-1 rhel-x86_64-server-5-mrg-grid-execute-1-beta rhel-x86_64-server-5-mrg-grid-execute-2 etc.
				// Bug 840102 - channels for rhel-<ARCH>-server-5-mrg-* are not yet mapped to product certs in rcm/rcm-metadata.git 
				bugIds.add("840102");	// CLOSED WONTFIX
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-hpc-5(-.*|$)")) {	// rhel-x86_64-server-hpc-5-beta
				// Bug 840103 - channel for rhel-x86_64-server-hpc-5-beta is not yet mapped to product cert in rcm/rcm-metadata.git
				bugIds.add("840103");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-rhev-hdk-2-5(-.+|$)")) {	// rhel-x86_64-server-rhev-hdk-2-5 rhel-x86_64-server-rhev-hdk-2-5-beta
				// Bug 840108 - channels for rhel-<ARCH>-rhev-hdk-2-5-* are not yet mapped to product certs in rcm/rhn-definitions.git
				bugIds.add("840108");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-productivity-5-beta(-.+|$)")) {	// rhel-x86_64-server-productivity-5-beta rhel-x86_64-server-productivity-5-beta-debuginfo
				// Bug 840136 - various rhel channels are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840136");	// CLOSED in favor of bug 840099
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");	// CLOSED WONTFIX
				bugIds.add("1105656");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-rhsclient-5(-.+|$)")) {	// rhel-x86_64-server-rhsclient-5 rhel-x86_64-server-rhsclient-5-debuginfo
				// Bug 840136 - various rhel channels are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840136");	// CLOSED in favor of bug 840099
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");	// CLOSED WONTFIX
				bugIds.add("1105656");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-xfs-5(-.+|$)")) {	// rhel-x86_64-server-xfs-5 rhel-x86_64-server-xfs-5-beta
				// Bug 840136 - various rhel channels are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840136");	// CLOSED in favor of bug 840099
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");	// CLOSED WONTFIX
				bugIds.add("1105656");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-5-shadow(-.+|$)")) {	// rhel-x86_64-server-5-shadow-debuginfo
				// Bug 840136 - various rhel channels are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840136");	// CLOSED in favor of bug 840099
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");	// CLOSED WONTFIX
				bugIds.add("1105656");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-eucjp-5(-.+|$)")) {	// rhel-x86_64-server-eucjp-5 rhel-x86_64-server-eucjp-5-beta etc.
				// Bug 840148 - missing product cert corresponding to "Red Hat EUCJP Support (for RHEL Server)"
				bugIds.add("840148");
				// Bug 847069 - Add certificates for rhel-x86_64-server-eucjp-5* channels.
				bugIds.add("847069");
			}
			if (rhnAvailableChildChannel.startsWith("rhx-")) {	// rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5 rhx-amanda-enterprise-backup-2.6-rhel-x86_64-server-5 etcetera
				// Bug 840111 - various rhx channels are not yet mapped to product certs in rcm/rcm-metadata.git 
				bugIds.add("840111");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-rhsclient-6(-.*|$)")) {	// rhel-x86_64-server-rhsclient-6 rhel-x86_64-server-rhsclient-6-debuginfo
				// Bug 872980 - channels for rhel-<ARCH>-server-rhsclient-6* are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("872980");	// CLOSED DUPLICATE of bug 818202
				bugIds.add("818202");	// CLOSED WONTFIX
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-6-ost-folsom(-.*|$)")) {	// rhel-x86_64-server-6-ost-folsom  rhel-x86_64-server-6-ost-folsom-debuginfo
				// Bug 872983 - channels for rhel-<ARCH>-server-6-ost-folsom* are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("872983");
				// Bug 884657 - the server-6-ost-folsom channels need to be mapped into channel-cert-mapping.txt
				bugIds.add("884657");
			}
			if (rhnAvailableChildChannel.startsWith("rhel-i386-server-sjis-6")) {	// rhel-i386-server-sjis-6 rhel-i386-server-sjis-6-debuginfo rhel-i386-server-sjis-6-beta rhel-i386-server-sjis-6-beta-debuginfo
				// Bug 892711 - rhel-i386-server-sjis-6 channels are available, but not accounted for in product-baseline.json
				bugIds.add("892711");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-client-dts-5(-.*|$)")) {	// rhel-i386-client-dts-5-debuginfo rhel-i386-client-dts-5-beta-debuginfo rhel-i386-client-dts-5-beta rhel-i386-client-dts-5
				// Bug 969156 - RHN Channels: [] in product-baseline.json is empty for "Red Hat Developer Toolset (for RHEL Client)"
				bugIds.add("969156");
				// Bug 969160 - rhel-*-client-dts-5* channels are not mapped in channel-cert-mapping.txt
				bugIds.add("969160");
			}
			if (rhnAvailableChildChannel.startsWith("rhel-i386-server-sjis-6-beta")) {	// rhel-i386-server-sjis-6-beta rhel-i386-server-sjis-6-beta-debuginfo
				// Bug 1009011 - some "Red Hat S-JIS Support (for RHEL Server)" channels are not mapped in product-certs.json
				bugIds.add("1009011");
			}
			if (rhnAvailableChildChannel.startsWith("rhel-x86_64-client-6-rhscl-1")) {	// rhel-x86_64-client-6-rhscl-1 rhel-x86_64-client-6-rhscl-1-debuginfo rhel-x86_64-client-6-rhscl-1-beta rhel-x86_64-client-6-rhscl-1-beta-debuginfo
				// Bug 1009071 - the RHN Classic rhel-x86_64-client-6-rhscl-1 channels are not accounted for in product-certs.json
				bugIds.add("1009071");
			}
			if (rhnAvailableChildChannel.matches(".+-htb(-.*|$)")) {
				// Bug 1011992 - High Touch Beta channel mappings should be excluded from channel-cert-mapping.txt
				bugIds.add("1011992");
			}
			List<String> variousAvailableChildChannels = Arrays.asList(new String[]{
			"rhel-x86_64-server-6-rhevm-3-beta",
			"rhel-x86_64-rhev-mgmt-agent-6-beta-debuginfo",
			"rhel-x86_64-rhev-mgmt-agent-6-beta",
			"rhel-x86_64-server-6-cf-ce-1-beta-debuginfo",
			"rhel-x86_64-server-6-cf-ce-1-beta",
			"rhel-x86_64-server-6-cf-se-1-beta-debuginfo",
			"rhel-x86_64-server-6-cf-se-1-beta",
			"rhel-x86_64-server-6-ose-1.2-infrastructure-debuginfo",
			"rhel-x86_64-server-6-ose-1.2-infrastructure",
			"rhel-x86_64-server-6-ose-1.2-jbosseap-debuginfo",
			"rhel-x86_64-server-6-ose-1.2-jbosseap",
			"rhel-x86_64-server-6-ose-1.2-node-debuginfo",
			"rhel-x86_64-server-6-ose-1.2-node",
			"rhel-x86_64-server-6-ose-1.2-rhc-debuginfo",
			"rhel-x86_64-server-6-ose-1.2-rhc",
			"rhel-x86_64-server-6-osop-1-infrastructure-beta-debuginfo",
			"rhel-x86_64-server-6-osop-1-infrastructure-beta",
			"rhel-x86_64-server-6-osop-1-jbosseap-beta-debuginfo",
			"rhel-x86_64-server-6-osop-1-jbosseap-beta",
			"rhel-x86_64-server-6-osop-1-node-beta-debuginfo",
			"rhel-x86_64-server-6-osop-1-node-beta",
			"rhel-x86_64-server-6-osop-1-rhc-beta-debuginfo",
			"rhel-x86_64-server-6-osop-1-rhc-beta",
			"rhel-x86_64-server-6-ost-3-cts-debuginfo",
			"rhel-x86_64-server-6-ost-3-cts",
			"rhel-x86_64-server-6-ost-3-debuginfo",
			"rhel-x86_64-server-6-ost-3",
			"rhel-x86_64-server-6-ovs-supplemental-beta-debuginfo",
			"rhel-x86_64-server-6-ovs-supplemental-beta",
			"rhel-x86_64-server-6-ovs-supplemental-debuginfo",
			"rhel-x86_64-server-6-ovs-supplemental",
			"rhel-x86_64-server-6-rhevh-beta-debuginfo",
			"rhel-x86_64-server-6-rhevh-beta",
			"rhel-x86_64-server-6-rhevm-3-beta-debuginfo",
			"rhel-x86_64-server-6-rhscl-1-beta-debuginfo",
			"rhel-x86_64-server-6-rhscl-1-beta",
			"rhel-x86_64-server-6-rhscl-1-debuginfo",
			"rhel-x86_64-server-6-rhscl-1",
			"rhel-x86_64-workstation-6-rhscl-1-beta",
			"rhel-x86_64-workstation-6-rhscl-1-beta-debuginfo",
			"rhel-i386-server-hts-6-beta",
			"rhel-x86_64-server-hts-6-beta",
			"rhel-ppc64-server-hts-6",	// Red Hat Hardware Certification Test Suite  productID=199
			"rhel-ppc64-server-hts-6-beta",});
			if (variousAvailableChildChannels.contains(rhnAvailableChildChannel)) {
				// Bug 1009109 - various available RHN Classic child channels that are not accounted for in product-certs.json
				bugIds.add("1009109");
			}
			
			if (rhnAvailableChildChannel.startsWith("rhel-x86_64-server-6-ost-4") ||
				rhnAvailableChildChannel.startsWith("rhel-x86_64-server-6-ost-beta")) {
				// Bug 1019981 - OpenStack-4.0 rhel-x86_64-server-6-ost-4 channel maps are missing
				bugIds.add("1019981");
			}
			
			if (rhnAvailableChildChannel.startsWith("rhel-x86_64-server-6-ose-2")) {
				// Bug 1019986 - OpenShift-2.0 rhel-x86_64-server-6-ose-2 channel maps are missing
				bugIds.add("1019986");
			}
			
			if (rhnAvailableChildChannel.startsWith("rhel-x86_64-server-6-rhs-rhsc-2.1")) {
				// Bug 1021661 - Red Hat Storage Management Console rhel-x86_64-server-6-rhs-rhsc-2.1 channel maps are missing
				bugIds.add("1021661");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-me-2") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-me-2-debuginfo")) {
				// Bug 1021664 - Red Hat CloudForms rhel-x86_64-server-6-cf-me-2 channel mappings are missing
				bugIds.add("1021664");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-7") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-supplementary-7") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-optional-7") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-optional-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-rh-common-7")) {
				// Bug 1078527 - channel-cert-mapping for ComputeNode rhel-7 product certs are missing and wrong
				bugIds.add("1078527");
			}
			
			if (
//				rhnAvailableChildChannel.equals("rhel-x86_64-server-productivity-5-beta") ||
//				rhnAvailableChildChannel.equals("rhel-x86_64-server-productivity-5-beta-debuginfo") ||
//				rhnAvailableChildChannel.equals("rhel-x86_64-server-xfs-5") ||
//				rhnAvailableChildChannel.equals("rhel-x86_64-server-xfs-5-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hts-5-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hts-5-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hts-5-beta-debuginfo") ||
//				rhnAvailableChildChannel.equals("rhel-x86_64-server-5-shadow-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-rhsclient-5") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-rhsclient-5-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-5-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-5-thirdparty-oracle-java-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-client-5-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-client-5-thirdparty-oracle-java-beta") ||
				rhnAvailableChildChannel.equals("rhel-i386-client-5-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("rhel-i386-client-5-thirdparty-oracle-java-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-5-cf-me-2")) {
				// Bug 1105656 - missing a few RHN Classic channel mappings to product certs
				bugIds.add("1105656");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-i386-server-productivity-5-beta") ||
				rhnAvailableChildChannel.equals("rhel-i386-server-productivity-5-beta-debuginfo")) {
				// Bug 1127794 - subscription-manager-migration-data is missing channel-to-productCert maps for rhel-i386-server-productivity-5-beta channels
				bugIds.add("1127794");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-me-3-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-me-3-beta-debuginfo")) {
				// Bug 1127880 - rhel-x86_64-server-6-cf-me-3-beta channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1127880");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-foreman") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-foreman-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-5") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-5-debuginfo")) {
				// Bug 1127884 - rhel-x86_64-server-6-ost-(5|foreman) channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1127884");
			}
			
			if (rhnAvailableChildChannel.startsWith("rhel-x86_64-server-6-rhs-3") ||
				rhnAvailableChildChannel.startsWith("rhel-x86_64-server-6-rhs-rhsc-3") ||
				rhnAvailableChildChannel.startsWith("rhel-x86_64-server-6-rhs-nagios-3")) {
				// Bug 1127900 - rhel-x86_64-server-6-rhs-(3|rhsc-3|nagios) channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1127900");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.5") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.5-debuginfo")) {
				// Bug 1127903 - rhel-x86_64-server-6-rhevm-3.5 channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1127903");
			}
			
			if (rhnAvailableChildChannel.startsWith("rhel-ppc-server-hts-5-beta")) {
				// Bug 1128283 - rhel-ppc-server-hts-5-beta channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1128283");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-s390x-server-hts-6-beta")) {
				// Bug 1131596 - rhel-s390x-server-hts-6-beta channel map is missing from channel-cert-mapping.txt
				bugIds.add("1131596");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-ppc-server-optional-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc-server-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-hpn-fastrack-6") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-hpn-fastrack-6-debuginfo")) {
				// Bug 1131629 - some rhel-ppc-server and rhel-ppc64-server channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1131629");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.4")) {
				// Bug 1129948 - RHEV 3.4 channel mappings missing for rhn-migrate-classic-to-rhsm
				bugIds.add("1129948");
			}
			
			if (//https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c0
				rhnAvailableChildChannel.equals("rhel-x86_64-server-sjis-6-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-eucjp-6") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-eucjp-6-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhs-optional-3") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhs-optional-3-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhs-bigdata-3") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhs-bigdata-3-debuginfo") ||
				//https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c1
				rhnAvailableChildChannel.equals("rhel-x86_64-server-sjis-6") ||
				// https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c2
				rhnAvailableChildChannel.equals("rhel-i386-server-sjis-6") ||
				rhnAvailableChildChannel.equals("rhel-i386-server-sjis-6-debuginfo") ||
				// https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c3
				rhnAvailableChildChannel.equals("rhel-ppc64-server-bluegene-6") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-bluegene-6-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-p7ih-6") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-p7ih-6-debuginfo") ||
				// https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c9
				rhnAvailableChildChannel.equals("rhel-x86_64-server-v2vwin-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-v2vwin-6-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-3-cts") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-3-cts-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.5") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.5-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ose-2.2-infrastructure") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ose-2.2-infrastructure-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cert") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cert-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.1-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.1-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hts-6-debuginfo") ||
				// https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c10
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-me-3.2") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-me-3.2-debuginfo") ||
				// https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c11
				rhnAvailableChildChannel.equals("rhel-i386-server-hts-6-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-i386-server-6-cert") ||
				rhnAvailableChildChannel.equals("rhel-i386-server-6-cert-beta") ||
				// https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c12
				rhnAvailableChildChannel.equals("rhel-ppc64-server-hts-6-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-dts-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-dts-6-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-6-cert") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-6-cert-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc-server-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc-server-optional-6-beta") ||
				// https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c13
				rhnAvailableChildChannel.equals("rhel-s390x-server-6-cert") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-6-cert-beta") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-hts-6-debuginfo") ||
				// https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c14
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-dts-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-dts-6-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-dts-6-debuginfo") ||
				// https://bugzilla.redhat.com/show_bug.cgi?id=1133942#c16
				rhnAvailableChildChannel.equals("rhel-x86_64-server-sap-hana-6") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-sap-hana-6-debuginfo")  ){
				// Bug 1133942 - various RHN channel maps to product certs missing in subscription-manager-migration-data
				bugIds.add("1133942");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-ppc64-server-dts-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-dts-6-beta-debuginfo")) {
				// Bug 1232442 - rhel-ppc64-server-dts-6-beta channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1232442");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-i386-server-hts-6-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-hts-6-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-hts-6-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hts-6-debuginfo")) {
				// Bug 1232448 - rhel-<ARCH>-server-hts-6-debuginfo channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1232448");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-dts-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-dts-6-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-dts-6-debuginfo")) {
				// Bug 1232458 - rhel-x86_64-hpc-node-dts-6 channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1232458");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-ppc-server-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc-server-optional-6-beta")) {
				// Bug 1232460 - rhel-ppc-server-6-beta channel map is missing from channel-cert-mapping.txt
				bugIds.add("1232460");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-i386-server-6-cert") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-6-cert") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-6-cert") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cert")) {
				// Bug 1232462 - rhel-<ARCH>-server-6-cert channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1232462");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-3-cts") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-3-cts-debuginfo")) {
				// Bug 1232465 - rhel-x86_64-server-6-ost-3-cts channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1232465");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.1-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.1-beta-debuginfo")) {
				// Bug 1232467 - rhel-x86_64-server-6-rhevm-3.1-beta channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1232467");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhs-optional-3") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhs-optional-3-debuginfo")) {
				// Bug 1232470 - rhel-x86_64-server-6-rhs-optional-3 channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1232470");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-sap-hana-6") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-sap-hana-6-debuginfo")) {
				// Bug 1232472 - rhel-x86_64-server-sap-hana-6 channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1232472");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-v2vwin-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-v2vwin-6-beta-debuginfo")) {
				// Bug 1232474 - rhel-x86_64-server-v2vwin-6-beta channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1232474");
			}
			
			if (//https://bugzilla.redhat.com/show_bug.cgi?id=1257212#c1
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hts-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hts-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-ha-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-ha-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-optional-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-optional-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-rs-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-rs-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-supplementary-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-supplementary-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-v2vwin-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-v2vwin-7-beta") ||
				//https://bugzilla.redhat.com/show_bug.cgi?id=1257212#c2
				rhnAvailableChildChannel.equals("rhel-ppc64-server-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-hts-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-hts-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-optional-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-optional-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-supplementary-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-ppc64-server-supplementary-7-beta-debuginfo") ||
				//https://bugzilla.redhat.com/show_bug.cgi?id=1257212#c3
				rhnAvailableChildChannel.equals("rhel-x86_64-client-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-client-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-client-optional-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-client-optional-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-client-supplementary-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-client-supplementary-7-beta-debuginfo") ||
				//https://bugzilla.redhat.com/show_bug.cgi?id=1257212#c4
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-optional-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-optional-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-supplementary-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-supplementary-7-beta-debuginfo") ||
				//https://bugzilla.redhat.com/show_bug.cgi?id=1257212#c5
				rhnAvailableChildChannel.equals("rhel-x86_64-workstation-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-workstation-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-workstation-optional-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-workstation-optional-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-workstation-supplementary-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-workstation-supplementary-7-beta-debuginfo") ||
				//https://bugzilla.redhat.com/show_bug.cgi?id=1257212#c7
				rhnAvailableChildChannel.equals("rhel-s390x-server-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-hts-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-hts-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-optional-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-optional-7-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-supplementary-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-supplementary-7-beta-debuginfo") ||
				//https://bugzilla.redhat.com/show_bug.cgi?id=1257212#c9
				rhnAvailableChildChannel.equals("rhel-x86_64-client-7-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-7-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-workstation-7-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("") ){
				// Bug 1257212 - various RHEL7 channel maps to product certs are missing in subscription-manager-migration-data
				bugIds.add("1257212");
			}
			
			if (//https://bugzilla.redhat.com/show_bug.cgi?id=1264470#c1
				rhnAvailableChildChannel.equals("rhel-ppc64-server-hts-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-hts-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hts-7-debuginfo") ||
				rhnAvailableChildChannel.equals("") ){
				// Bug 1264470 - various RHEL7 channel maps to product certs are missing in subscription-manager-migration-data
				bugIds.add("1264470");
			}
			
			if (//https://bugzilla.redhat.com/show_bug.cgi?id=1462980#c1
				rhnAvailableChildChannel.equals("rhel-ppc64-server-hts-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-hts-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hts-7-debuginfo") ||
				rhnAvailableChildChannel.equals("") ){
				// Bug 1462980 - various RHEL7 channel maps to product certs are missing in subscription-manager-migration-data
				bugIds.add("1462980");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rh-gluster-3-samba-debuginfo")) {
				// Bug 1286842 - 'rhel-x86_64-server-6-rh-gluster-3-samba-debuginfo' channel map is missing from channel-cert-mapping.txt
				bugIds.add("1286842");
				// Bug 1561715 - 'rhel-x86_64-server-6-rh-gluster-3-samba-debuginfo' channel map is missing from channel-cert-mapping.txt
				bugIds.add("1561715");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-ce-1-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-ce-1-beta-debuginfo")) {
				// Bug 1299620 - rhel-x86_64-server-6-cf-ce-1-beta channel maps are absent from channel-cert-mapping.txt 
				bugIds.add("1299620");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-se-1-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-se-1-beta-debuginfo")) {
				// Bug 1299621 - rhel-x86_64-server-6-cf-se-1-beta channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1299621");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-tools-1-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-cf-tools-1-beta-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-i386-server-6-cf-tools-1-beta") ||
				rhnAvailableChildChannel.equals("rhel-i386-server-6-cf-tools-1-beta-debuginfo")) {
				// Bug 1299623 - rhel-x86_64-server-6-cf-tools-1-beta channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1299623");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-rhev-mgmt-agent-6-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-rhev-mgmt-agent-6-beta-debuginfo")) {
				// Bug 1299624 - rhel-x86_64-rhev-mgmt-agent-6-beta channel maps are absent from channel-cert-mapping.txt 
				bugIds.add("1299624");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-rhev-mgmt-agent-7") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-rhev-mgmt-agent-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-rhev-mgmt-agent-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-rhev-mgmt-agent-7-beta-debuginfo")) {
				// Bug 1300848 - RHN channels to product cert maps for "rhel-x86_64-rhev-mgmt-agent-7*" disappeared
				bugIds.add("1300848");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.6") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevm-3.6-debuginfo")) {
				// Bug 1320592 - rhel-x86_64-server-6-rhevm-3.6 channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1320592");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-hpn-fastrack-6") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hpn-fastrack-6-debuginfo")) {
				// Bug 1320597 - rhel-x86_64-server-hpn-fastrack-6 channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1320597");
				// Bug 1561724 - rhel-x86_64-server-hpn-fastrack-6 channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1561724");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-hpn-6") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-hpn-6-debuginfo")) {
				// Bug 1320607 - rhel-x86_64-server-hpn-6 channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1320607");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-7") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-7-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-7-optools") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-7-optools-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-7-director") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-7-director-debuginfo")) {
				// Bug 1328628 - rhel-x86_64-server-7-ost-7 channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1328628");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh-beta-debuginfo")) {
				// Bug 1333545 - rhel-x86_64-server-7-rhevh channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1333545");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh-beta-debuginfo")) {
				// Bug 1462994 - rhel-x86_64-server-7-rhevh channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1462994");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-director") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-director-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-optools") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-optools-debuginfo")) {
				// Bug 1349533 - rhel-x86_64-server-7-ost-8 channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1349533");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-director") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-director-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-optools") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-8-optools-debuginfo")) {
				// Bug 1462984 - rhel-x86_64-server-7-ost-8 channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1462984");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-s390x-server-ha-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-ha-7-beta-debuginfo")) {
				// Bug 1354653 - rhel-s390x-server-ha-7-beta channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1354653");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-s390x-server-rs-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-s390x-server-rs-7-beta-debuginfo")) {
				// Bug 1354655 - rhel-s390x-server-rs-7-beta channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1354655");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rh-gluster-3-client") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rh-gluster-3-client-debuginfo")) {
				// Bug 1349538 - rhel-x86_64-server-7-rh-gluster-3-client channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1349538");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rh-gluster-3-client") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rh-gluster-3-client-debuginfo")) {
				// Bug 1462989 - rhel-x86_64-server-7-rh-gluster-3-client channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1462989");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rh-gluster-3-nfs") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rh-gluster-3-nfs-debuginfo")) {
				// Bug 1393557 - rhel-x86_64-server-6-rh-gluster-3-nfs channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1393557");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-ost-beta-debuginfo")) {
				// Bug 1393563 - rhel-x86_64-server-6-ost-beta channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1393563");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevh-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-6-rhevh-beta-debuginfo")) {
				// Bug 1418476 - rhel-x86_64-server-6-rhevh-beta channel maps are missing from channel-cert-mapping.txt
				bugIds.add("1418476");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-rhevh-beta-debuginfo")) {
				// Bug 1435255 - RHN channels to product cert maps for "rhel-x86_64-server-7-rhevh-beta*" disappeared
				bugIds.add("1435255");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-rhev-mgmt-agent-7-beta") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-rhev-mgmt-agent-7-beta-debuginfo")) {
				// Bug 1435245 - RHN channels to product cert maps for "rhel-x86_64-rhev-mgmt-agent-7-beta*" disappeared
				bugIds.add("1435245");
			}
			
			if (rhnAvailableChildChannel.startsWith("rhel-x86_64-server-7-cf-me-4")) {
				// Bug 1519975 - rhel-x86_64-server-7-cf-me-4.* channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1519975");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-9") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-10") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-11") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-12") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-9-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-10-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-11-debuginfo") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-ost-12-debuginfo")) {
				// Bug 1519979 - rhel-x86_64-server-7-ost-[9|10|11|12] channel maps are absent from channel-cert-mapping.txt
				bugIds.add("1519979");
			}
			
			if (rhnAvailableChildChannel.equals("rhel-x86_64-client-7-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-server-7-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-workstation-7-thirdparty-oracle-java") ||
				rhnAvailableChildChannel.equals("rhel-x86_64-hpc-node-7-thirdparty-oracle-java")) {
				// Bug 1550219 - missing RHN channel mappings for rhel-x86_64-<VARIANT>-7-thirdparty-oracle-java
				bugIds.add("1550219");
			}
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[]{blockedByBzBug,	rhnAvailableChildChannel}));
		}
		
		return ll;
	}
}




// Notes ***********************************************************************

